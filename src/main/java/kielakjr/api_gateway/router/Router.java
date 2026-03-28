package kielakjr.api_gateway.router;

import kielakjr.api_gateway.config.RouteConfig;
import java.util.List;
import kielakjr.api_gateway.loadbalancer.LoadBalancer;
import kielakjr.api_gateway.loadbalancer.LoadBalancerStrategy;
import kielakjr.api_gateway.loadbalancer.RoundRobinLoadBalancer;
import kielakjr.api_gateway.loadbalancer.WeightedRandomLoadBalancer;

public class Router {
  private List<RouteConfig> routes;
  private LoadBalancer loadBalancer;

  public Router(List<RouteConfig> routes) {
    this(routes, LoadBalancerStrategy.ROUND_ROBIN);
  }

  public Router(List<RouteConfig> routes, LoadBalancerStrategy loadBalancerStrategy) {
    this.routes = routes;
    this.loadBalancer = switch (loadBalancerStrategy) {
      case ROUND_ROBIN -> new RoundRobinLoadBalancer();
      case WEIGHTED_RANDOM -> new WeightedRandomLoadBalancer();
      default -> throw new IllegalStateException("Unsupported load balancer strategy: " + loadBalancerStrategy);
    };
  }

  public String resolve(String path) throws IllegalStateException {
    if (routes == null || routes.isEmpty()) {
      throw new IllegalStateException("No routes configured");
    }
    for (RouteConfig route : routes) {
      if (path.startsWith(route.getPath())) {
        List<String> upstreams = route.getUpstreams();
        if (upstreams == null || upstreams.isEmpty()) {
          throw new IllegalStateException("No upstream servers configured for route: " + route.getPath());
        }
        return loadBalancer.nextUpstream(upstreams);
      }
    }
    return null;
  }
}
