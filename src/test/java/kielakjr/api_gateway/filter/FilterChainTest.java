package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FilterChainTest {

  private boolean executeChain(FilterChain chain, FullHttpRequest request) {
    AtomicBoolean result = new AtomicBoolean();
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
  void execute_emptyChain_returnsTrue() {
    FilterChain chain = new FilterChain(List.of());

    assertTrue(executeChain(chain, createRequest()));
  }

  @Test
  void execute_allFiltersPass_returnsTrue() {
    Filter passing1 = (c, r) -> true;
    Filter passing2 = (c, r) -> true;
    FilterChain chain = new FilterChain(List.of(passing1, passing2));

    assertTrue(executeChain(chain, createRequest()));
  }

  @Test
  void execute_firstFilterFails_returnsFalse() {
    Filter failing = (c, r) -> false;
    Filter passing = (c, r) -> true;
    FilterChain chain = new FilterChain(List.of(failing, passing));

    assertFalse(executeChain(chain, createRequest()));
  }

  @Test
  void execute_secondFilterFails_returnsFalse() {
    Filter passing = (c, r) -> true;
    Filter failing = (c, r) -> false;
    FilterChain chain = new FilterChain(List.of(passing, failing));

    assertFalse(executeChain(chain, createRequest()));
  }

  @Test
  void execute_shortCircuits_doesNotCallFiltersAfterFailure() {
    AtomicBoolean secondCalled = new AtomicBoolean(false);
    Filter failing = (c, r) -> false;
    Filter second = (c, r) -> { secondCalled.set(true); return true; };
    FilterChain chain = new FilterChain(List.of(failing, second));

    executeChain(chain, createRequest());

    assertFalse(secondCalled.get());
  }

  @Test
  void execute_callsFiltersInOrder() {
    StringBuilder order = new StringBuilder();
    Filter first = (c, r) -> { order.append("1"); return true; };
    Filter second = (c, r) -> { order.append("2"); return true; };
    Filter third = (c, r) -> { order.append("3"); return true; };
    FilterChain chain = new FilterChain(List.of(first, second, third));

    executeChain(chain, createRequest());

    assertEquals("123", order.toString());
  }
}
