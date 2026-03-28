package kielakjr.api_gateway;

import kielakjr.api_gateway.config.ConfigLoader;
import kielakjr.api_gateway.config.GatewayConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

  @Test
  void configFileExists_andParsesSuccessfully() throws Exception {
    ConfigLoader loader = new ConfigLoader();
    GatewayConfig config = loader.loadConfig("src/test/resources/test-config.yaml");

    assertNotNull(config);
    assertNotNull(config.getServer());
    assertNotNull(config.getRoutes());
    assertFalse(config.getRoutes().isEmpty());
  }
}
