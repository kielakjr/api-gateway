package kielakjr.api_gateway.proxy;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyResponseTest {

  @Test
  void constructor_setsStatusCode() {
    ProxyResponse response = new ProxyResponse(200, "body".getBytes(), "text/plain");

    assertEquals(200, response.getStatusCode());
  }

  @Test
  void constructor_setsBody() {
    byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
    ProxyResponse response = new ProxyResponse(200, body, "text/plain");

    assertArrayEquals(body, response.getBody());
  }

  @Test
  void constructor_setsContentType() {
    ProxyResponse response = new ProxyResponse(200, "body".getBytes(), "application/json");

    assertEquals("application/json", response.getContentType());
  }

  @Test
  void constructor_nullBody_returnsNull() {
    ProxyResponse response = new ProxyResponse(404, null, "text/plain");

    assertNull(response.getBody());
  }

  @Test
  void constructor_variousStatusCodes() {
    assertEquals(500, new ProxyResponse(500, new byte[0], "text/plain").getStatusCode());
    assertEquals(302, new ProxyResponse(302, new byte[0], "text/plain").getStatusCode());
  }
}
