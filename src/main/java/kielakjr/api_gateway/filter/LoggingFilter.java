package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class LoggingFilter implements Filter {
  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request) {
    System.out.println("Incoming request: " + request.uri());
    System.out.println("Method: " + request.method());
    return true;
  }
}
