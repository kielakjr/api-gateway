package kielakjr.api_gateway.loadbalancer;

import java.util.List;

public class RoundRobinLoadBalancer implements LoadBalancer {
  private int currentIndex = 0;

  @Override
  public synchronized String nextUpstream(List<String> upstreams) {
    if (upstreams.isEmpty()) {
      throw new IllegalStateException("No upstream servers available");
    }
    String upstream = upstreams.get(currentIndex);
    currentIndex = (currentIndex + 1) % upstreams.size();
    return upstream;
  }

}
