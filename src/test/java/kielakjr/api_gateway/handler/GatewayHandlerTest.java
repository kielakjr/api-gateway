package kielakjr.api_gateway.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import kielakjr.api_gateway.config.RouteConfig;
import kielakjr.api_gateway.filter.FilterChain;
import kielakjr.api_gateway.router.Router;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatewayHandlerTest {

  private Router router;

  @BeforeEach
  void setUp() {
    RouteConfig usersRoute = new RouteConfig();
    usersRoute.setPath("/api/users");
    usersRoute.setUpstreams(List.of("http://localhost:9001"));

    router = new Router(List.of(usersRoute));
  }

  private EmbeddedChannel createChannel(FilterChain filterChain) {
    return new EmbeddedChannel(new GatewayHandler(router, filterChain));
  }

  private FullHttpResponse sendRequest(EmbeddedChannel channel, String uri) {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    channel.writeInbound(request);
    return channel.readOutbound();
  }

  @Test
  void channelRead0_matchingRoute_returns200() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/api/users");

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals("http://localhost:9001", response.content().toString(CharsetUtil.UTF_8));
    response.release();
  }

  @Test
  void channelRead0_noMatchingRoute_returns404() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/unknown");

    assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
    assertEquals(0, response.content().readableBytes());
    response.release();
  }

  @Test
  void channelRead0_filterRejects_noResponseFromHandler() {
    FilterChain chain = new FilterChain(List.of((ctx, req) -> false));
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    channel.writeInbound(request);

    FullHttpResponse response = channel.readOutbound();
    assertNull(response);
  }

  @Test
  void channelRead0_matchingRoute_setsContentType() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/api/users");

    assertEquals("text/plain; charset=UTF-8", response.headers().get(HttpHeaderNames.CONTENT_TYPE));
    response.release();
  }

  @Test
  void channelRead0_matchingRoute_setsContentLength() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/api/users");

    int expectedLength = "http://localhost:9001".length();
    assertEquals(expectedLength, response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH));
    response.release();
  }

  @Test
  void channelRead0_notFoundRoute_setsContentLengthZero() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/unknown");

    assertEquals(0, response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH));
    response.release();
  }

  @Test
  void channelRead0_keepAlive_setsConnectionHeader() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    HttpUtil.setKeepAlive(request, true);
    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();

    assertEquals("keep-alive", response.headers().get(HttpHeaderNames.CONNECTION));
    response.release();
  }

  @Test
  void channelRead0_notKeepAlive_closesConnection() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    HttpUtil.setKeepAlive(request, false);
    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();

    assertNull(response.headers().get(HttpHeaderNames.CONNECTION));
    response.release();
  }

  @Test
  void channelRead0_subpathMatch_returns200() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendRequest(channel, "/api/users/123");

    assertEquals(HttpResponseStatus.OK, response.status());
    response.release();
  }

  @Test
  void channelRead0_postWithBody_returns200() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/users",
        Unpooled.copiedBuffer("{\"name\":\"test\"}", CharsetUtil.UTF_8)
    );
    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    response.release();
  }

  @Test
  void channelRead0_multipleRequestsSameChannel_allSucceed() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request1 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    HttpUtil.setKeepAlive(request1, true);
    channel.writeInbound(request1);
    FullHttpResponse response1 = channel.readOutbound();
    assertEquals(HttpResponseStatus.OK, response1.status());
    response1.release();

    FullHttpRequest request2 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users/456");
    HttpUtil.setKeepAlive(request2, true);
    channel.writeInbound(request2);
    FullHttpResponse response2 = channel.readOutbound();
    assertEquals(HttpResponseStatus.OK, response2.status());
    response2.release();
  }

  @Test
  void exceptionCaught_closesChannel() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    channel.pipeline().fireExceptionCaught(new RuntimeException("test error"));

    assertFalse(channel.isActive());
  }
}
