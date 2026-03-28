package kielakjr.api_gateway.filter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class RateLimitFilter implements Filter {
  private final int maxRequestsPerMinute;
  private ConcurrentHashMap<String, TokenBucket> requestCounts;

  public RateLimitFilter(int maxRequestsPerMinute) {
    this.maxRequestsPerMinute = maxRequestsPerMinute;
    this.requestCounts = new ConcurrentHashMap<>();
  }

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request) {
    String clientId = getClientId(ctx);
    requestCounts.computeIfAbsent(clientId, k -> new TokenBucket(maxRequestsPerMinute));
    return requestCounts.get(clientId).consume();
  }

  private String getClientId(ChannelHandlerContext ctx) {
    return ctx.channel().remoteAddress().toString();
  }

  private class TokenBucket {
    private AtomicLong tokens;
    private int capacity;
    private long lastRefillTime;

    public TokenBucket(int capacity) {
      this.capacity = capacity;
      this.lastRefillTime = System.currentTimeMillis();
      this.tokens = new AtomicLong(capacity);
    }

    public synchronized boolean consume() {
      refill();
      if (tokens.get() > 0) {
        tokens.decrementAndGet();
        return true;
      }
      return false;
    }

    private void refill() {
      long now = System.currentTimeMillis();
      long elapsed = now - lastRefillTime;
      if (elapsed > 60000) {
        tokens = new AtomicLong(capacity);
        lastRefillTime = now;
      }
    }
  }
}
