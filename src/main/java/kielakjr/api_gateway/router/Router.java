package kielakjr.api_gateway.router;

import kielakjr.api_gateway.config.RouteConfig;
import java.util.List;
import kielakjr.api_gateway.loadbalancer.LoadBalancerStrategy;
import kielakjr.api_gateway.loadbalancer.RoundRobinLoadBalancer;
import kielakjr.api_gateway.loadbalancer.WeightedRandomLoadBalancer;

public class Router {
  private List<RouteConfig> routes;
  private LoadBalancerStrategy loadBalancerStrategy;

  public Router(List<RouteConfig> routes, LoadBalancerStrategy loadBalancerStrategy) {
    this.routes = routes;
    this.loadBalancerStrategy = loadBalancerStrategy;
  }

  public String resolve(String path) {
    if (routes == null || routes.isEmpty()) {
      throw new IllegalStateException("No routes configured");
    }
    for (RouteConfig route : routes) {
      if (path.startsWith(route.getPath())) {
        List<String> upstreams = route.getUpstreams();
        if (upstreams == null || upstreams.isEmpty()) {
          throw new IllegalStateException("No upstream servers configured for route: " + route.getPath());
        }
        switch (loadBalancerStrategy) {
          case ROUND_ROBIN:
            return new RoundRobinLoadBalancer().nextUpstream(upstreams);
          case WEIGHTED_RANDOM:
            return new WeightedRandomLoadBalancer().nextUpstream(upstreams);
          default:
            throw new IllegalStateException("Unsupported load balancer strategy: " + loadBalancerStrategy);
        }
      }
    }
    return null;
  }
}
