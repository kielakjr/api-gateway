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
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

  private record FilterResult(boolean passed, FullHttpResponse response) {}

  private FilterResult applyFilter(RateLimitFilter filter, FullHttpRequest request) {
    AtomicBoolean result = new AtomicBoolean();

    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(filter.apply(ctx, request));
      }
    });
    channel.attr(AttributeKey.valueOf("test")).set("dummy");
    channel.pipeline().context(channel.pipeline().first()).channel();

    EmbeddedChannel channelWithAddress = new EmbeddedChannelWithAddress(
        new InetSocketAddress("127.0.0.1", 12345),
        new ChannelInboundHandlerAdapter() {
          @Override
          public void channelRead(ChannelHandlerContext ctx, Object msg) {
            result.set(filter.apply(ctx, request));
          }
        }
    );

    channelWithAddress.writeInbound(request);
    FullHttpResponse response = channelWithAddress.readOutbound();
    return new FilterResult(result.get(), response);
  }

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

    EmbeddedChannelWithAddress client1Channel = new EmbeddedChannelWithAddress(
        new InetSocketAddress("10.0.0.1", 1111),
        new ChannelInboundHandlerAdapter()
    );
    EmbeddedChannelWithAddress client2Channel = new EmbeddedChannelWithAddress(
        new InetSocketAddress("10.0.0.2", 2222),
        new ChannelInboundHandlerAdapter()
    );

    AtomicBoolean result1 = new AtomicBoolean();
    client1Channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result1.set(filter.apply(ctx, createRequest()));
      }
    });
    client1Channel.writeInbound(createRequest());
    assertTrue(result1.get());
    FullHttpResponse resp1 = client1Channel.readOutbound();
    if (resp1 != null) resp1.release();

    AtomicBoolean result2 = new AtomicBoolean();
    client2Channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result2.set(filter.apply(ctx, createRequest()));
      }
    });
    client2Channel.writeInbound(createRequest());
    assertTrue(result2.get(), "Different client should have its own rate limit");
    FullHttpResponse resp2 = client2Channel.readOutbound();
    if (resp2 != null) resp2.release();
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
}
