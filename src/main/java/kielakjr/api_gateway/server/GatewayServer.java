package kielakjr.api_gateway.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import kielakjr.api_gateway.handler.GatewayHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.ChannelOption;
import io.github.cdimascio.dotenv.Dotenv;
import io.netty.bootstrap.ServerBootstrap;

import java.util.List;
import kielakjr.api_gateway.config.RouteConfig;
import kielakjr.api_gateway.router.Router;
import kielakjr.api_gateway.filter.FilterChain;
import kielakjr.api_gateway.filter.LoggingFilter;
import kielakjr.api_gateway.filter.AuthFilter;
import kielakjr.api_gateway.filter.RateLimitFilter;

public class GatewayServer {

  private final int port;
  private final Router router;
  private final FilterChain filterChain;


  public GatewayServer(int port, List<RouteConfig> routes) {
    this.port = port;
    this.router = new Router(routes);
    Dotenv dotenv = Dotenv.load();
    this.filterChain = new FilterChain(List.of(new LoggingFilter(), new AuthFilter(dotenv.get("JWT_SECRET")), new RateLimitFilter(2)));
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
            ch.pipeline().addLast(new GatewayHandler(router, filterChain));
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
