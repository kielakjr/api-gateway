package kielakjr.api_gateway.filter;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
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
    TokenBucket tokenBucket = requestCounts.computeIfAbsent(clientId, k -> new TokenBucket(maxRequestsPerMinute));
    if (tokenBucket.consume()) {
      return true;
    } else {
      writeTooManyRequestsResponse(ctx);
      return false;
    }
  }

  private String getClientId(ChannelHandlerContext ctx) {
    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    return socketAddress.getHostString();
  }

  private void writeTooManyRequestsResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.TOO_MANY_REQUESTS,
      Unpooled.copiedBuffer("Too Many Requests", CharsetUtil.UTF_8)
    );
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
        tokens.set(capacity);
        lastRefillTime = now;
      }
    }
  }
}
