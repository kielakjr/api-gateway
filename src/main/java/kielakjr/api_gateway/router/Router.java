package kielakjr.api_gateway.router;

import kielakjr.api_gateway.config.RouteConfig;
import java.util.List;

public class Router {
  private List<RouteConfig> routes;

  public Router(List<RouteConfig> routes) {
    this.routes = routes;
  }

  public String resolve(String path) {
    for (RouteConfig route : routes) {
      if (path.startsWith(route.getPath())) {
        return route.getUpstreams().get(0);
      }
    }
    return null;
  }
}
