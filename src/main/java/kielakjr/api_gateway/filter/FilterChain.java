package kielakjr.api_gateway.filter;

import java.util.List;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class FilterChain {
  private final List<Filter> filters;

  public FilterChain(List<Filter> filters) {
    this.filters = filters;
  }

  public boolean execute(ChannelHandlerContext ctx, FullHttpRequest request) {
    for (Filter filter : filters) {
      if (!filter.apply(ctx, request)) {
        return false;
      }
    }
    return true;
  }

}
