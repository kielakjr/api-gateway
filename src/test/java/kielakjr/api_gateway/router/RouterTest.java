package kielakjr.api_gateway.router;

import kielakjr.api_gateway.config.RouteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

  private Router router;

  @BeforeEach
  void setUp() {
    RouteConfig usersRoute = new RouteConfig();
    usersRoute.setPath("/api/users");
    usersRoute.setUpstreams(List.of("http://localhost:9001"));

    RouteConfig ordersRoute = new RouteConfig();
    ordersRoute.setPath("/api/orders");
    ordersRoute.setUpstreams(List.of("http://localhost:9002", "http://localhost:9003"));

    router = new Router(List.of(usersRoute, ordersRoute));
  }

  @Test
  void resolve_exactPathMatch_returnsUpstream() {
    String result = router.resolve("/api/users");

    assertNotNull(result);
    assertEquals("http://localhost:9001", result);
  }

  @Test
  void resolve_prefixMatch_returnsUpstream() {
    String result = router.resolve("/api/users/123");

    assertNotNull(result);
    assertEquals("http://localhost:9001", result);
  }

  @Test
  void resolve_noMatch_returnsEmpty() {
    String result = router.resolve("/api/products");

    assertNull(result);
  }

  @Test
  void resolve_rootPath_returnsEmpty() {
    String result = router.resolve("/");

    assertNull(result);
  }

  @Test
  void resolve_multipleUpstreams_returnsFirstUpstream() {
    String result = router.resolve("/api/orders");

    assertNotNull(result);
    assertEquals("http://localhost:9002", result);
  }

  @Test
  void resolve_firstMatchWins() {
    RouteConfig broad = new RouteConfig();
    broad.setPath("/api");
    broad.setUpstreams(List.of("http://localhost:8001"));

    RouteConfig specific = new RouteConfig();
    specific.setPath("/api/users");
    specific.setUpstreams(List.of("http://localhost:8002"));

    Router routerWithOverlap = new Router(List.of(broad, specific));

    String result = routerWithOverlap.resolve("/api/users");

    assertNotNull(result);
    assertEquals("http://localhost:8001", result, "Should match the first route in the list");
  }

  @Test
  void resolve_emptyRoutes_returnsEmpty() {
    Router emptyRouter = new Router(List.of());

    String result = emptyRouter.resolve("/api/users");

    assertNull(result);
  }
}
