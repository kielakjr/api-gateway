package kielakjr.api_gateway.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import kielakjr.api_gateway.handler.GatewayHandler;
import kielakjr.api_gateway.metrics.MetricsRegistry;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.ChannelOption;
import io.github.cdimascio.dotenv.Dotenv;
import io.netty.bootstrap.ServerBootstrap;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import kielakjr.api_gateway.config.GatewayConfig;
import kielakjr.api_gateway.router.Router;
import kielakjr.api_gateway.filter.FilterChain;
import kielakjr.api_gateway.filter.LoggingFilter;
import kielakjr.api_gateway.proxy.ProxyClient;
import kielakjr.api_gateway.filter.AuthFilter;
import kielakjr.api_gateway.filter.RateLimitFilter;
import kielakjr.api_gateway.config.TimeoutsConfig;
import kielakjr.api_gateway.filter.CorsFilter;
import kielakjr.api_gateway.filter.RequestTransformFilter;
import kielakjr.api_gateway.handler.SecurityHeadersHandler;
import kielakjr.api_gateway.config.ConfigWatcher;

public class GatewayServer {

  private final int port;
  private final FilterChain filterChain;
  private final ProxyClient proxyClient;
  private final TimeoutsConfig timeoutsConfig;
  private final MetricsRegistry metricsRegistry;
  private final AtomicReference<Router> routerRef = new AtomicReference<>();
  private final ConfigWatcher configWatcher;


  public GatewayServer(GatewayConfig config, MetricsRegistry metricsRegistry) {
    this.port = config.getServer().getPort();
    this.routerRef.set(new Router(config.getRoutes(), config.getLoadBalancerStrategy()));
    Dotenv dotenv = Dotenv.load();
    this.filterChain = new FilterChain(List.of(new LoggingFilter(), new CorsFilter(config.getCors()), new AuthFilter(dotenv.get("JWT_SECRET")), new RateLimitFilter(config.getRateLimitPerMinute()), new RequestTransformFilter()), metricsRegistry);
    this.proxyClient = new ProxyClient(config.getConnectionPool(), config.getCircuitBreaker(), config.getRetryPolicy());
    this.timeoutsConfig = config.getTimeouts();
    this.metricsRegistry = metricsRegistry;
    this.configWatcher = new ConfigWatcher("config.yaml", routerRef);
    try {
      this.configWatcher.start();
    } catch (Exception e) {
      throw new RuntimeException("Failed to start configuration watcher: " + e.getMessage());
    }
  }

  public void run() throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast(new HttpObjectAggregator(1048576));
            ch.pipeline().addLast(new SecurityHeadersHandler());
            ch.pipeline().addLast(new ReadTimeoutHandler(timeoutsConfig.getReadSeconds(), TimeUnit.SECONDS));
            ch.pipeline().addLast(new WriteTimeoutHandler(timeoutsConfig.getWriteSeconds(), TimeUnit.SECONDS));
            ch.pipeline().addLast(new GatewayHandler(routerRef, filterChain, proxyClient, metricsRegistry));
          }
        })
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.bind(port).sync();
        f.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

}
