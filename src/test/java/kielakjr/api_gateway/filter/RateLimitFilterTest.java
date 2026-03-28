package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import kielakjr.api_gateway.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

  private record FilterResult(boolean passed, FullHttpResponse response) {}

  private static class EmbeddedChannelWithAddress extends EmbeddedChannel {
    private final InetSocketAddress remoteAddress;

    EmbeddedChannelWithAddress(InetSocketAddress remoteAddress, io.netty.channel.ChannelHandler... handlers) {
      super(handlers);
      this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return remoteAddress;
    }
  }

  private FilterResult applyFilter(RateLimitFilter filter, FullHttpRequest request) {
    return applyFilter(filter, request, "127.0.0.1");
  }

  private FilterResult applyFilter(RateLimitFilter filter, FullHttpRequest request, String clientIp) {
    AtomicBoolean result = new AtomicBoolean();
    RequestContext rctx = new RequestContext(clientIp);

    EmbeddedChannelWithAddress channel = new EmbeddedChannelWithAddress(
        new InetSocketAddress(clientIp, 12345),
        new ChannelInboundHandlerAdapter() {
          @Override
          public void channelRead(ChannelHandlerContext ctx, Object msg) {
            result.set(filter.apply(ctx, request, rctx));
          }
        }
    );

    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();
    return new FilterResult(result.get(), response);
  }

  private FullHttpRequest createRequest() {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
  }

  @Test
  void apply_firstRequest_allowed() {
    RateLimitFilter filter = new RateLimitFilter(10);

    FilterResult result = applyFilter(filter, createRequest());

    assertTrue(result.passed());
    assertNull(result.response());
  }

  @Test
  void apply_underLimit_allAllowed() {
    RateLimitFilter filter = new RateLimitFilter(5);

    for (int i = 0; i < 5; i++) {
      FilterResult result = applyFilter(filter, createRequest());
      assertTrue(result.passed(), "Request " + (i + 1) + " should be allowed");
      if (result.response() != null) result.response().release();
    }
  }

  @Test
  void apply_exceedsLimit_blocked() {
    RateLimitFilter filter = new RateLimitFilter(3);

    for (int i = 0; i < 3; i++) {
      FilterResult result = applyFilter(filter, createRequest());
      assertTrue(result.passed());
      if (result.response() != null) result.response().release();
    }

    FilterResult result = applyFilter(filter, createRequest());
    assertFalse(result.passed());
  }

  @Test
  void apply_exceedsLimit_returns429() {
    RateLimitFilter filter = new RateLimitFilter(1);

    FilterResult first = applyFilter(filter, createRequest());
    assertTrue(first.passed());
    if (first.response() != null) first.response().release();

    FilterResult result = applyFilter(filter, createRequest());
    assertNotNull(result.response());
    assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, result.response().status());
    result.response().release();
  }

  @Test
  void apply_differentClients_trackedSeparately() {
    RateLimitFilter filter = new RateLimitFilter(1);

    FilterResult result1 = applyFilter(filter, createRequest(), "10.0.0.1");
    assertTrue(result1.passed());
    if (result1.response() != null) result1.response().release();

    FilterResult result2 = applyFilter(filter, createRequest(), "10.0.0.2");
    assertTrue(result2.passed(), "Different client should have its own rate limit");
    if (result2.response() != null) result2.response().release();
  }

  @Test
  void apply_limitOfOne_secondRequestBlocked() {
    RateLimitFilter filter = new RateLimitFilter(1);

    FilterResult first = applyFilter(filter, createRequest());
    assertTrue(first.passed());
    if (first.response() != null) first.response().release();

    FilterResult second = applyFilter(filter, createRequest());
    assertFalse(second.passed());
    if (second.response() != null) second.response().release();
  }

  @Test
  void apply_429Response_containsBody() {
    RateLimitFilter filter = new RateLimitFilter(1);

    FilterResult first = applyFilter(filter, createRequest());
    assertTrue(first.passed());
    if (first.response() != null) first.response().release();

    FilterResult result = applyFilter(filter, createRequest());
    assertNotNull(result.response());
    String body = result.response().content().toString(CharsetUtil.UTF_8);
    assertEquals("Too Many Requests", body);
    result.response().release();
  }
}
