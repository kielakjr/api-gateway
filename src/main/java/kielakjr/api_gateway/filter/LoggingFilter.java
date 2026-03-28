package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.context.RequestContext;

public class LoggingFilter implements Filter {
  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    System.out.println("Incoming request: " + request.uri());
    System.out.println("Method: " + request.method());
    System.out.println("Request ID: " + rctx.getRequestId());
    return true;
  }
}
