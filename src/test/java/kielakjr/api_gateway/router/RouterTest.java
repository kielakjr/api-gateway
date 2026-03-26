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
    Optional<String> result = router.resolve("/api/users");

    assertTrue(result.isPresent());
    assertEquals("http://localhost:9001", result.get());
  }

  @Test
  void resolve_prefixMatch_returnsUpstream() {
    Optional<String> result = router.resolve("/api/users/123");

    assertTrue(result.isPresent());
    assertEquals("http://localhost:9001", result.get());
  }

  @Test
  void resolve_noMatch_returnsEmpty() {
    Optional<String> result = router.resolve("/api/products");

    assertTrue(result.isEmpty());
  }

  @Test
  void resolve_rootPath_returnsEmpty() {
    Optional<String> result = router.resolve("/");

    assertTrue(result.isEmpty());
  }

  @Test
  void resolve_multipleUpstreams_returnsFirstUpstream() {
    Optional<String> result = router.resolve("/api/orders");

    assertTrue(result.isPresent());
    assertEquals("http://localhost:9002", result.get());
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

    Optional<String> result = routerWithOverlap.resolve("/api/users");

    assertTrue(result.isPresent());
    assertEquals("http://localhost:8001", result.get(), "Should match the first route in the list");
  }

  @Test
  void resolve_emptyRoutes_returnsEmpty() {
    Router emptyRouter = new Router(List.of());

    Optional<String> result = emptyRouter.resolve("/api/users");

    assertTrue(result.isEmpty());
  }
}
