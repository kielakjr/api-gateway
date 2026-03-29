package kielakjr.api_gateway.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

  @Test
  void constructor_setsClientIp() {
    RequestContext ctx = new RequestContext("192.168.1.1");

    assertEquals("192.168.1.1", ctx.getClientIp());
  }

  @Test
  void constructor_generatesRequestId() {
    RequestContext ctx = new RequestContext("127.0.0.1");

    assertNotNull(ctx.getRequestId());
    assertFalse(ctx.getRequestId().isEmpty());
  }

  @Test
  void constructor_generatesUniqueIds() {
    RequestContext ctx1 = new RequestContext("127.0.0.1");
    RequestContext ctx2 = new RequestContext("127.0.0.1");

    assertNotEquals(ctx1.getRequestId(), ctx2.getRequestId());
  }

  @Test
  void constructor_setsStartTimeNanos() {
    long before = System.nanoTime();
    RequestContext ctx = new RequestContext("127.0.0.1");
    long after = System.nanoTime();

    assertTrue(ctx.getStartTimeNanos() >= before);
    assertTrue(ctx.getStartTimeNanos() <= after);
  }

  @Test
  void resolvedUpstream_defaultsToNull() {
    RequestContext ctx = new RequestContext("127.0.0.1");

    assertNull(ctx.getResolvedUpstream());
  }

  @Test
  void setResolvedUpstream_setsValue() {
    RequestContext ctx = new RequestContext("127.0.0.1");

    ctx.setResolvedUpstream("http://localhost:9001");

    assertEquals("http://localhost:9001", ctx.getResolvedUpstream());
  }

  @Test
  void matchedRoute_defaultsToNull() {
    RequestContext ctx = new RequestContext("127.0.0.1");

    assertNull(ctx.getMatchedRoute());
  }

  @Test
  void setMatchedRoute_setsValue() {
    RequestContext ctx = new RequestContext("127.0.0.1");

    ctx.setMatchedRoute("/api/users");

    assertEquals("/api/users", ctx.getMatchedRoute());
  }
}
