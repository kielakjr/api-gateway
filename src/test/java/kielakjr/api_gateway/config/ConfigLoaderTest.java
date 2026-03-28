package kielakjr.api_gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

  private ConfigLoader configLoader;
  private String testConfigPath;

  @BeforeEach
  void setUp() {
    configLoader = new ConfigLoader();
    testConfigPath = new File("src/test/resources/test-config.yaml").getAbsolutePath();
  }

  @Test
  void loadConfig_parsesServerPort() throws Exception {
    GatewayConfig config = configLoader.loadConfig(testConfigPath);

    assertEquals(9090, config.getServer().getPort());
  }

  @Test
  void loadConfig_parsesRoutes() throws Exception {
    GatewayConfig config = configLoader.loadConfig(testConfigPath);

    List<RouteConfig> routes = config.getRoutes();
    assertEquals(2, routes.size());
  }

  @Test
  void loadConfig_parsesRoutePaths() throws Exception {
    GatewayConfig config = configLoader.loadConfig(testConfigPath);

    List<RouteConfig> routes = config.getRoutes();
    assertEquals("/api/users", routes.get(0).getPath());
    assertEquals("/api/orders", routes.get(1).getPath());
  }

  @Test
  void loadConfig_parsesUpstreams() throws Exception {
    GatewayConfig config = configLoader.loadConfig(testConfigPath);

    List<RouteConfig> routes = config.getRoutes();
    assertEquals(List.of("http://localhost:9001"), routes.get(0).getUpstreams());
    assertEquals(List.of("http://localhost:9002", "http://localhost:9003"), routes.get(1).getUpstreams());
  }

  @Test
  void loadConfig_invalidPath_throwsException() {
    assertThrows(Exception.class, () -> configLoader.loadConfig("nonexistent.yaml"));
  }

  @Test
  void loadConfig_malformedYaml_throwsException() {
    String malformedPath = new File("src/test/resources/malformed-config.yaml").getAbsolutePath();

    assertThrows(Exception.class, () -> configLoader.loadConfig(malformedPath));
  }

  @Test
  void loadConfig_missingServer_returnsNullServer() throws Exception {
    String path = new File("src/test/resources/missing-server-config.yaml").getAbsolutePath();

    GatewayConfig config = configLoader.loadConfig(path);

    assertNull(config.getServer());
  }

  @Test
  void loadConfig_emptyRoutes_returnsEmptyList() throws Exception {
    String path = new File("src/test/resources/empty-routes-config.yaml").getAbsolutePath();

    GatewayConfig config = configLoader.loadConfig(path);

    assertNotNull(config.getRoutes());
    assertTrue(config.getRoutes().isEmpty());
  }
}
