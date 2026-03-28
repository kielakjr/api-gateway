package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import kielakjr.api_gateway.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FilterChainTest {

  private RequestContext executeChain(FilterChain chain, FullHttpRequest request) {
    AtomicReference<RequestContext> result = new AtomicReference<>();
    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(chain.execute(ctx, request));
      }
    });
    channel.writeInbound(request);
    return result.get();
  }

  private FullHttpRequest createRequest() {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");
  }

  @Test
  void execute_emptyChain_returnsNonNull() {
    FilterChain chain = new FilterChain(List.of());

    assertNotNull(executeChain(chain, createRequest()));
  }

  @Test
  void execute_allFiltersPass_returnsNonNull() {
    Filter passing1 = (c, r, rctx) -> true;
    Filter passing2 = (c, r, rctx) -> true;
    FilterChain chain = new FilterChain(List.of(passing1, passing2));

    assertNotNull(executeChain(chain, createRequest()));
  }

  @Test
  void execute_firstFilterFails_returnsNull() {
    Filter failing = (c, r, rctx) -> false;
    Filter passing = (c, r, rctx) -> true;
    FilterChain chain = new FilterChain(List.of(failing, passing));

    assertNull(executeChain(chain, createRequest()));
  }

  @Test
  void execute_secondFilterFails_returnsNull() {
    Filter passing = (c, r, rctx) -> true;
    Filter failing = (c, r, rctx) -> false;
    FilterChain chain = new FilterChain(List.of(passing, failing));

    assertNull(executeChain(chain, createRequest()));
  }

  @Test
  void execute_shortCircuits_doesNotCallFiltersAfterFailure() {
    AtomicBoolean secondCalled = new AtomicBoolean(false);
    Filter failing = (c, r, rctx) -> false;
    Filter second = (c, r, rctx) -> { secondCalled.set(true); return true; };
    FilterChain chain = new FilterChain(List.of(failing, second));

    executeChain(chain, createRequest());

    assertFalse(secondCalled.get());
  }

  @Test
  void execute_callsFiltersInOrder() {
    StringBuilder order = new StringBuilder();
    Filter first = (c, r, rctx) -> { order.append("1"); return true; };
    Filter second = (c, r, rctx) -> { order.append("2"); return true; };
    Filter third = (c, r, rctx) -> { order.append("3"); return true; };
    FilterChain chain = new FilterChain(List.of(first, second, third));

    executeChain(chain, createRequest());

    assertEquals("123", order.toString());
  }
}
