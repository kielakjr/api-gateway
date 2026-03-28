package kielakjr.api_gateway.handler;

import com.sun.net.httpserver.HttpServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import kielakjr.api_gateway.config.RouteConfig;
import kielakjr.api_gateway.filter.FilterChain;
import kielakjr.api_gateway.router.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GatewayHandlerTest {

  private static HttpServer upstream;
  private static int upstreamPort;
  private Router router;

  @BeforeAll
  static void startUpstream() throws Exception {
    upstream = HttpServer.create(new InetSocketAddress(0), 0);
    upstreamPort = upstream.getAddress().getPort();

    upstream.createContext("/", exchange -> {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().toString();
      byte[] requestBody = exchange.getRequestBody().readAllBytes();

      String response = String.format(
          "{\"method\":\"%s\",\"path\":\"%s\",\"bodyLength\":%d}",
          method, path, requestBody.length);

      byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });

    upstream.start();
  }

  @AfterAll
  static void stopUpstream() {
    upstream.stop(0);
  }

  @BeforeEach
  void setUp() {
    RouteConfig usersRoute = new RouteConfig();
    usersRoute.setPath("/api/users");
    usersRoute.setUpstreams(List.of("http://localhost:" + upstreamPort));

    router = new Router(List.of(usersRoute));
  }

  private EmbeddedChannel createChannel(FilterChain filterChain) {
    return new EmbeddedChannel(new GatewayHandler(router, filterChain));
  }

  private FullHttpResponse sendRequest(EmbeddedChannel channel, HttpMethod method, String uri) {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
    channel.writeInbound(request);
    return awaitOutboundResponse(channel);
  }

  private FullHttpResponse awaitOutboundResponse(EmbeddedChannel channel) {
    return awaitOutboundResponse(channel, 1000);
  }

  private FullHttpResponse awaitOutboundResponse(EmbeddedChannel channel, long timeoutMs) {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

    while (System.nanoTime() < deadline) {
      channel.runPendingTasks();
      channel.runScheduledPendingTasks();

      FullHttpResponse response = channel.readOutbound();
      if (response != null) {
        return response;
      }

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Interrupted while waiting for async response", e);
      }
    }

    return null;
  }

  private FullHttpResponse sendGetRequest(EmbeddedChannel channel, String uri) {
    return sendRequest(channel, HttpMethod.GET, uri);
  }

  @Test
  void channelRead0_matchingRoute_returns200() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/api/users");

    assertEquals(HttpResponseStatus.OK, response.status());
    String body = response.content().toString(CharsetUtil.UTF_8);
    assertTrue(body.contains("\"method\":\"GET\""));
    response.release();
  }

  @Test
  void channelRead0_proxiedResponse_containsUpstreamPath() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/api/users");

    String body = response.content().toString(CharsetUtil.UTF_8);
    assertTrue(body.contains("\"path\":\"/api/users\""));
    response.release();
  }

  @Test
  void channelRead0_subpath_forwardsFullUri() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/api/users/123");

    assertEquals(HttpResponseStatus.OK, response.status());
    String body = response.content().toString(CharsetUtil.UTF_8);
    assertTrue(body.contains("\"path\":\"/api/users/123\""));
    response.release();
  }

  @Test
  void channelRead0_noMatchingRoute_returns404() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/unknown");

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

    FullHttpResponse response = awaitOutboundResponse(channel, 150);
    assertNull(response);
  }

  @Test
  void channelRead0_matchingRoute_setsContentType() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/api/users");

    assertEquals("text/plain; charset=UTF-8", response.headers().get(HttpHeaderNames.CONTENT_TYPE));
    response.release();
  }

  @Test
  void channelRead0_matchingRoute_setsContentLength() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/api/users");

    int contentLength = response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH);
    assertEquals(response.content().readableBytes(), contentLength);
    response.release();
  }

  @Test
  void channelRead0_notFoundRoute_setsContentLengthZero() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpResponse response = sendGetRequest(channel, "/unknown");

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
    FullHttpResponse response = awaitOutboundResponse(channel);

    assertEquals("keep-alive", response.headers().get(HttpHeaderNames.CONNECTION));
    response.release();
  }

  @Test
  void channelRead0_notKeepAlive_noConnectionHeader() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    HttpUtil.setKeepAlive(request, false);
    channel.writeInbound(request);
    FullHttpResponse response = awaitOutboundResponse(channel);

    assertNull(response.headers().get(HttpHeaderNames.CONNECTION));
    response.release();
  }

  @Test
  void channelRead0_postWithBody_forwardsToUpstream() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/users",
        Unpooled.copiedBuffer("{\"name\":\"test\"}", CharsetUtil.UTF_8));
    channel.writeInbound(request);
    FullHttpResponse response = awaitOutboundResponse(channel);

    assertEquals(HttpResponseStatus.OK, response.status());
    String body = response.content().toString(CharsetUtil.UTF_8);
    assertTrue(body.contains("\"method\":\"POST\""));
    assertTrue(body.contains("\"bodyLength\":15"));
    response.release();
  }

  @Test
  void channelRead0_multipleRequestsSameChannel_allSucceed() {
    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = createChannel(chain);

    FullHttpRequest request1 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users");
    HttpUtil.setKeepAlive(request1, true);
    channel.writeInbound(request1);
    FullHttpResponse response1 = awaitOutboundResponse(channel);
    assertEquals(HttpResponseStatus.OK, response1.status());
    response1.release();

    FullHttpRequest request2 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/users/456");
    HttpUtil.setKeepAlive(request2, true);
    channel.writeInbound(request2);
    FullHttpResponse response2 = awaitOutboundResponse(channel);
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

  @Test
  void channelRead0_unreachableUpstream_returns404() {
    RouteConfig deadRoute = new RouteConfig();
    deadRoute.setPath("/api/dead");
    deadRoute.setUpstreams(List.of("http://localhost:1"));
    Router deadRouter = new Router(List.of(deadRoute));

    FilterChain chain = new FilterChain(List.of());
    EmbeddedChannel channel = new EmbeddedChannel(new GatewayHandler(deadRouter, chain));

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/dead");
    channel.writeInbound(request);
    FullHttpResponse response = awaitOutboundResponse(channel);

    assertEquals(HttpResponseStatus.BAD_GATEWAY, response.status());
    response.release();
  }
}
