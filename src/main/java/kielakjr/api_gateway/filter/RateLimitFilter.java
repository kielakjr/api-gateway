package kielakjr.api_gateway.filter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import kielakjr.api_gateway.context.RequestContext;

public class RateLimitFilter implements Filter {
  private final int maxRequestsPerMinute;
  private ConcurrentHashMap<String, TokenBucket> requestCounts;
  private AtomicInteger requests;

  public RateLimitFilter(int maxRequestsPerMinute) {
    this.maxRequestsPerMinute = maxRequestsPerMinute;
    this.requestCounts = new ConcurrentHashMap<>();
    this.requests = new AtomicInteger(0);
  }

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    String clientId = rctx.getClientIp();
    TokenBucket tokenBucket = requestCounts.computeIfAbsent(clientId, k -> new TokenBucket(maxRequestsPerMinute));
    if (requests.incrementAndGet() >= 100) {
      requests.set(0);
      requestCounts.entrySet().removeIf(entry -> {
        long idleTime = System.currentTimeMillis() - entry.getValue().getLastAccessTime();
        return idleTime > 10 * 60000;
      });
    }
    if (tokenBucket.consume()) {
      rctx.setRateLimitInfo(tokenBucket.getTokens(), tokenBucket.getCapacity());
      return true;
    } else {
      rctx.setStatusCode(HttpResponseStatus.TOO_MANY_REQUESTS.code());
      rctx.setRateLimitInfo(tokenBucket.getTokens(), tokenBucket.getCapacity(), TokenBucket.REFILL_INTERVAL / 1000);
      writeTooManyRequestsResponse(ctx, rctx);
      return false;
    }
  }

  private void writeTooManyRequestsResponse(ChannelHandlerContext ctx, RequestContext rctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.TOO_MANY_REQUESTS,
      Unpooled.copiedBuffer("Too Many Requests", CharsetUtil.UTF_8)
    );
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().set("X-RateLimit-Limit", rctx.getRateLimitInfo().getCapacity());
    response.headers().set("X-RateLimit-Remaining", rctx.getRateLimitInfo().getTokens());
    response.headers().set("X-RateLimit-Reset", rctx.getRateLimitInfo().getRefillSeconds());
    if (rctx.getCorsOrigin() != null) {
      response.headers().set("Access-Control-Allow-Origin", rctx.getCorsOrigin());
    }

    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private class TokenBucket {
    private AtomicLong tokens;
    private int capacity;
    private long lastRefillTime;
    private long lastAccessTime;
    private static final long REFILL_INTERVAL = 60000;

    public TokenBucket(int capacity) {
      this.capacity = capacity;
      this.lastRefillTime = System.currentTimeMillis();
      this.tokens = new AtomicLong(capacity);
      this.lastAccessTime = System.currentTimeMillis();
    }

    public synchronized boolean consume() {
      updateLastAccessTime();
      refill();
      if (tokens.get() > 0) {
        tokens.decrementAndGet();
        return true;
      }
      return false;
    }

    private synchronized void refill() {
      long now = System.currentTimeMillis();
      long elapsed = now - lastRefillTime;
      if (elapsed > REFILL_INTERVAL) {
        tokens.set(capacity);
        lastRefillTime = now;
      }
    }

    public long getTokens() {
      return tokens.get();
    }

    public long getCapacity() {
      return capacity;
    }

    public long getLastAccessTime() {
      return lastAccessTime;
    }

    public void updateLastAccessTime() {
      this.lastAccessTime = System.currentTimeMillis();
    }
  }
}
