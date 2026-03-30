package kielakjr.api_gateway.filter;

import java.net.InetSocketAddress;
import java.util.List;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.metrics.MetricsCollector;
import kielakjr.api_gateway.metrics.MetricsRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kielakjr.api_gateway.context.RequestContext;

public class FilterChain {
  private final List<Filter> filters;
  private final MetricsCollector metricsCollector;
  private final MetricsRegistry metricsRegistry;
  private final Logger log = LoggerFactory.getLogger(FilterChain.class);

  public FilterChain(List<Filter> filters, MetricsRegistry metricsRegistry) {
    this.filters = filters;
    this.metricsRegistry = metricsRegistry;
    this.metricsCollector = new MetricsCollector(metricsRegistry);
  }

  public RequestContext execute(ChannelHandlerContext ctx, FullHttpRequest request) {
    RequestContext requestContext = new RequestContext(this.getClientId(ctx));
    for (Filter filter : filters) {
      if (!filter.apply(ctx, request, requestContext)) {
        metricsCollector.recordRequest(requestContext.getStartTimeNanos(), requestContext.getStatusCode());
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
