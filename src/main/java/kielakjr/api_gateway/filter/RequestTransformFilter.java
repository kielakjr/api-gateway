package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.context.RequestContext;

public class RequestTransformFilter implements Filter {
  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    request.headers().set("X-Request-ID", rctx.getRequestId());
    request.headers().set("X-Client-IP", rctx.getClientIp());
    request.headers().set("X-Gateway-Version", "1.0");
    request.headers().remove("Authorization");
    return true;
  }
}
