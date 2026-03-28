package kielakjr.api_gateway.filter;

import java.net.InetSocketAddress;
import java.util.List;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import kielakjr.api_gateway.context.RequestContext;

public class FilterChain {
  private final List<Filter> filters;

  public FilterChain(List<Filter> filters) {
    this.filters = filters;
  }

  public RequestContext execute(ChannelHandlerContext ctx, FullHttpRequest request) {
    RequestContext requestContext = new RequestContext(this.getClientId(ctx));
    for (Filter filter : filters) {
      if (!filter.apply(ctx, request, requestContext)) {
        return null;
      }
    }
    return requestContext;
  }

  private String getClientId(ChannelHandlerContext ctx) {
    var remoteAddress = ctx.channel().remoteAddress();
    if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
      return inetSocketAddress.getHostString();
    }
    return "unknown";
  }
}
