package kielakjr.api_gateway;

import kielakjr.api_gateway.config.GatewayConfig;
import kielakjr.api_gateway.server.GatewayServer;
import kielakjr.api_gateway.config.ConfigLoader;

public class App {
  public static void main(String[] args) throws Exception {
    GatewayConfig config = new ConfigLoader().loadConfig("config.yaml");
    GatewayServer server = new GatewayServer(config.getServer().getPort(), config.getRoutes(), config.getRateLimitPerMinute());
    server.run();
  }
}
