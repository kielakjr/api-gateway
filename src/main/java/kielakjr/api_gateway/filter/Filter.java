package kielakjr.api_gateway.filter;

import kielakjr.api_gateway.context.RequestContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface Filter {
  boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext requestContext);
}
