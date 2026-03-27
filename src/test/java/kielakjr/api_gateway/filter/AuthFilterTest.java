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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AuthFilterTest {

  private static final String RAW_SECRET = "this-is-a-test-secret-key-that-is-at-least-32-bytes-long";
  private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes());

  private AuthFilter authFilter;

  @BeforeEach
  void setUp() {
    authFilter = new AuthFilter(BASE64_SECRET);
  }

  private String generateValidToken() {
    SecretKey key = Keys.hmacShaKeyFor(RAW_SECRET.getBytes());
    return Jwts.builder()
        .subject("testuser")
        .signWith(key)
        .compact();
  }

  private FullHttpRequest createRequest(String authHeaderValue) {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
    if (authHeaderValue != null) {
      request.headers().set("Authorization", authHeaderValue);
    }
    return request;
  }

  private record FilterResult(boolean passed, FullHttpResponse response) {}

  private FilterResult applyFilter(FullHttpRequest request) {
    AtomicBoolean result = new AtomicBoolean();
    AtomicReference<FullHttpRequest> requestRef = new AtomicReference<>(request);

    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        result.set(authFilter.apply(ctx, requestRef.get()));
      }
    });

    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();
    return new FilterResult(result.get(), response);
  }

  @Test
  void apply_validToken_returnsTrue() {
    String token = generateValidToken();
    FullHttpRequest request = createRequest("Bearer " + token);

    FilterResult result = applyFilter(request);

    assertTrue(result.passed());
    assertNull(result.response());
  }

  @Test
  void apply_missingAuthHeader_returnsFalse() {
    FullHttpRequest request = createRequest(null);

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_missingAuthHeader_writes401Response() {
    FullHttpRequest request = createRequest(null);

    FilterResult result = applyFilter(request);

    assertNotNull(result.response());
    assertEquals(HttpResponseStatus.UNAUTHORIZED, result.response().status());
    result.response().release();
  }

  @Test
  void apply_noBearerPrefix_returnsFalse() {
    FullHttpRequest request = createRequest("Basic sometoken");

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_emptyBearerToken_returnsFalse() {
    FullHttpRequest request = createRequest("Bearer ");

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_invalidToken_returnsFalse() {
    FullHttpRequest request = createRequest("Bearer not.a.valid.jwt");

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_tokenSignedWithWrongKey_returnsFalse() {
    SecretKey wrongKey = Keys.hmacShaKeyFor("a-completely-different-secret-key-that-is-long-enough".getBytes());
    String token = Jwts.builder()
        .subject("testuser")
        .signWith(wrongKey)
        .compact();
    FullHttpRequest request = createRequest("Bearer " + token);

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }

  @Test
  void apply_bearerPrefixOnly_returnsFalse() {
    FullHttpRequest request = createRequest("Bearer");

    FilterResult result = applyFilter(request);

    assertFalse(result.passed());
  }
}
