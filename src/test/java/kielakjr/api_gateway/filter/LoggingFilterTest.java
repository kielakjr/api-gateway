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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LoggingFilterTest {

  private boolean applyFilter(LoggingFilter filter, FullHttpRequest request) {
    AtomicBoolean result = new AtomicBoolean();
    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(filter.apply(ctx, request, new RequestContext("127.0.0.1")));
      }
    });
    channel.writeInbound(request);
    return result.get();
  }

  @Test
  void apply_alwaysReturnsTrue() {
    LoggingFilter filter = new LoggingFilter();
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");

    assertTrue(applyFilter(filter, request));
  }

  @Test
  void apply_returnsTrue_forPostRequest() {
    LoggingFilter filter = new LoggingFilter();
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/users");

    assertTrue(applyFilter(filter, request));
  }
}
