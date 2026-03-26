package kielakjr.api_gateway.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import kielakjr.api_gateway.handler.GatewayHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.ChannelOption;

import io.netty.bootstrap.ServerBootstrap;

public class GatewayServer {

  private final int port;

  public GatewayServer(int port) {
    this.port = port;
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
            ch.pipeline().addLast(new GatewayHandler());
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
