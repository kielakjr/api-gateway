package kielakjr.api_gateway.router;

import kielakjr.api_gateway.config.RouteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    assertThrows(IllegalStateException.class, () -> emptyRouter.resolve("/api/users"));
  }

  @Test
  void resolve_pathWithQueryString_matchesRoute() {
    String result = router.resolve("/api/users?page=1&size=10");

    assertNotNull(result);
    assertEquals("http://localhost:9001", result);
  }

  @Test
  void resolve_pathWithTrailingSlash_matchesRoute() {
    String result = router.resolve("/api/users/");

    assertNotNull(result);
    assertEquals("http://localhost:9001", result);
  }

  @Test
  void resolve_caseSensitive_doesNotMatchDifferentCase() {
    String result = router.resolve("/API/USERS");

    assertNull(result);
  }

  @Test
  void resolve_nullPath_throwsException() {
    assertThrows(NullPointerException.class, () -> router.resolve(null));
  }

  @Test
  void getRoutesAsJson_returnsAllRoutes() {
    String json = router.getRoutesAsJson();

    assertTrue(json.contains("\"path\":\"/api/users\""));
    assertTrue(json.contains("\"path\":\"/api/orders\""));
  }

  @Test
  void getRoutesAsJson_containsUpstreams() {
    String json = router.getRoutesAsJson();

    assertTrue(json.contains("\"http://localhost:9001\""));
    assertTrue(json.contains("\"http://localhost:9002\""));
    assertTrue(json.contains("\"http://localhost:9003\""));
  }

  @Test
  void getRoutesAsJson_validArrayFormat() {
    String json = router.getRoutesAsJson();

    assertTrue(json.startsWith("["));
    assertTrue(json.endsWith("]"));
    assertFalse(json.contains(",]"));
    assertFalse(json.contains(",}"));
  }

  @Test
  void getRoutesAsJson_emptyRoutes_returnsEmptyArray() {
    Router emptyRouter = new Router(List.of());

    assertEquals("[]", emptyRouter.getRoutesAsJson());
  }

  @Test
  void getRoutesAsJson_singleRoute_exactJson() {
    RouteConfig route = new RouteConfig();
    route.setPath("/api/test");
    route.setUpstreams(List.of("http://localhost:8080"));
    Router singleRouter = new Router(List.of(route));

    String json = singleRouter.getRoutesAsJson();

    assertEquals("[{\"path\":\"/api/test\",\"upstreams\":[\"http://localhost:8080\"]}]", json);
  }
}
