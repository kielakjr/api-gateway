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
import kielakjr.api_gateway.config.CorsConfig;
import kielakjr.api_gateway.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CorsFilterTest {

  private CorsConfig corsConfig;
  private CorsFilter corsFilter;

  @BeforeEach
  void setUp() {
    corsConfig = new CorsConfig();
    corsConfig.setAllowedOrigins(List.of("http://localhost:3000", "https://example.com"));
    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    corsConfig.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    corsConfig.setMaxAgeSeconds(3600);
    corsFilter = new CorsFilter(corsConfig);
  }

  private record FilterResult(boolean passed, FullHttpResponse response, RequestContext rctx) {}

  private FilterResult applyFilter(FullHttpRequest request) {
    AtomicBoolean result = new AtomicBoolean();
    RequestContext rctx = new RequestContext("127.0.0.1");

    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(corsFilter.apply(ctx, request, rctx));
      }
    });

    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();
    return new FilterResult(result.get(), response, rctx);
  }

  @Test
  void apply_noOriginHeader_passesThrough() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");

    FilterResult result = applyFilter(request);

    assertTrue(result.passed());
    assertNull(result.response());
  }

  @Test
  void apply_originWithGetRequest_passesThrough() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertTrue(result.passed());
    assertNull(result.response());
  }

  @Test
  void apply_originWithGetRequest_setsCorsOriginOnContext() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertEquals("http://localhost:3000", result.rctx().getCorsOrigin());
  }

  @Test
  void apply_optionsRequest_returnsFalse() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_optionsRequest_returns204() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals(HttpResponseStatus.NO_CONTENT, result.response().status());
    result.response().release();
  }

  @Test
  void apply_optionsRequest_setsAllowOriginHeader() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals("http://localhost:3000, https://example.com",
        result.response().headers().get("Access-Control-Allow-Origin"));
    result.response().release();
  }

  @Test
  void apply_optionsRequest_setsAllowMethodsHeader() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals("GET, POST, PUT, DELETE",
        result.response().headers().get("Access-Control-Allow-Methods"));
    result.response().release();
  }

  @Test
  void apply_optionsRequest_setsAllowHeadersHeader() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals("Content-Type, Authorization",
        result.response().headers().get("Access-Control-Allow-Headers"));
    result.response().release();
  }

  @Test
  void apply_optionsRequest_setsMaxAgeHeader() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals("3600", result.response().headers().get("Access-Control-Max-Age"));
    result.response().release();
  }

  @Test
  void apply_optionsRequest_setsContentLengthZero() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "http://localhost:3000");

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals(0, result.response().headers().getInt("Content-Length"));
    assertEquals(0, result.response().content().readableBytes());
    result.response().release();
  }

  @Test
  void apply_optionsWithoutOrigin_passesThrough() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");

    FilterResult result = applyFilter(request);

    assertTrue(result.passed());
    assertNull(result.response());
  }

  @Test
  void apply_postWithOrigin_passesThrough() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/test");
    request.headers().set("Origin", "https://example.com");

    FilterResult result = applyFilter(request);

    assertTrue(result.passed());
    assertEquals("https://example.com", result.rctx().getCorsOrigin());
  }

  @Test
  void apply_singleAllowedOrigin_setsCorrectHeader() {
    corsConfig.setAllowedOrigins(List.of("https://only.com"));
    CorsFilter singleOriginFilter = new CorsFilter(corsConfig);

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/api/test");
    request.headers().set("Origin", "https://only.com");

    AtomicBoolean result = new AtomicBoolean();
    RequestContext rctx = new RequestContext("127.0.0.1");
    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(singleOriginFilter.apply(ctx, request, rctx));
      }
    });
    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();

    assertNotNull(response);
    assertEquals("https://only.com", response.headers().get("Access-Control-Allow-Origin"));
    response.release();
  }
}
