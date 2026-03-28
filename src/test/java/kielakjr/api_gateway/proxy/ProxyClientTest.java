package kielakjr.api_gateway.proxy;

import com.sun.net.httpserver.HttpServer;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyClientTest {

  private static HttpServer upstream;
  private static int port;
  private final ProxyClient proxyClient = new ProxyClient();

  @BeforeAll
  static void startUpstream() throws Exception {
    upstream = HttpServer.create(new InetSocketAddress(0), 0);
    port = upstream.getAddress().getPort();

    upstream.createContext("/", exchange -> {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().toString();
      byte[] requestBody = exchange.getRequestBody().readAllBytes();

      String response = String.format(
          "{\"method\":\"%s\",\"path\":\"%s\",\"bodyLength\":%d}",
          method, path, requestBody.length
      );

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

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private FullHttpRequest createNettyRequest(HttpMethod method, String uri) {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
  }

  private FullHttpRequest createNettyRequest(HttpMethod method, String uri, String body) {
    return new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1, method, uri,
        Unpooled.copiedBuffer(body, CharsetUtil.UTF_8)
    );
  }

  @Test
  void forwardRequest_get_returnsUpstreamResponse() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.GET, "/api/users");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertEquals(200, response.getStatusCode());
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"method\":\"GET\""));
    assertTrue(body.contains("\"path\":\"/api/users\""));
    request.release();
  }

  @Test
  void forwardRequest_post_forwardsBody() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.POST, "/api/users", "{\"name\":\"test\"}");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertEquals(200, response.getStatusCode());
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"method\":\"POST\""));
    assertTrue(body.contains("\"bodyLength\":15"));
    request.release();
  }

  @Test
  void forwardRequest_put_forwardsMethodAndBody() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.PUT, "/api/users/1", "{\"name\":\"updated\"}");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertEquals(200, response.getStatusCode());
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"method\":\"PUT\""));
    assertTrue(body.contains("\"path\":\"/api/users/1\""));
    request.release();
  }

  @Test
  void forwardRequest_delete_forwardsMethod() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.DELETE, "/api/users/1");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertEquals(200, response.getStatusCode());
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"method\":\"DELETE\""));
    request.release();
  }

  @Test
  void forwardRequest_subpath_preservesFullUri() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.GET, "/api/users/123/orders");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertEquals(200, response.getStatusCode());
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"path\":\"/api/users/123/orders\""));
    request.release();
  }

  @Test
  void forwardRequest_emptyBody_sendsZeroLength() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.GET, "/test");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"bodyLength\":0"));
    request.release();
  }

  @Test
  void forwardRequest_unreachableUpstream_throwsException() {
    FullHttpRequest request = createNettyRequest(HttpMethod.GET, "/test");

    assertThrows(Exception.class, () ->
        proxyClient.forwardRequest("http://localhost:1", request)
    );
    request.release();
  }

  @Test
  void forwardRequest_responseBody_isNotEmpty() throws Exception {
    FullHttpRequest request = createNettyRequest(HttpMethod.GET, "/test");

    ProxyResponse response = proxyClient.forwardRequest(baseUrl(), request);

    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);
    request.release();
  }
}
