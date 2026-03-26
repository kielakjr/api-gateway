package kielakjr.api_gateway.config;
import java.util.List;

public class GatewayConfig {
  private ServerConfig server;
  private List<RouteConfig> routes;

  public ServerConfig getServer() {
    return server;
  }

  public void setServer(ServerConfig server) {
    this.server = server;
  }

  public List<RouteConfig> getRoutes() {
    return routes;
  }

  public void setRoutes(List<RouteConfig> routes) {
    this.routes = routes;
  }
}
