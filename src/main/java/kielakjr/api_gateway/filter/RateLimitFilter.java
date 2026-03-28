package kielakjr.api_gateway.filter;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class RateLimitFilter implements Filter {
  private final int maxRequestsPerMinute;
  private ConcurrentHashMap<String, Integer> requestCounts;

  public RateLimitFilter(int maxRequestsPerMinute) {
    this.maxRequestsPerMinute = maxRequestsPerMinute;
    this.requestCounts = new ConcurrentHashMap<>();
  }

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request) {
    String clientId = getClientId(ctx);
    requestCounts.putIfAbsent(clientId, maxRequestsPerMinute);
    int remainingRequests = requestCounts.get(clientId);
    if (remainingRequests > 0) {
      requestCounts.put(clientId, remainingRequests - 1);
      return true;
    } else {
      return false;
    }

  }

  private String getClientId(ChannelHandlerContext ctx) {
    return ctx.channel().remoteAddress().toString();
  }
}
