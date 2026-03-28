package kielakjr.api_gateway.loadbalancer;

import java.util.List;

public class WeightedRandomLoadBalancer implements LoadBalancer {
  @Override
  public String nextUpstream(List<String> upstreams) {
    if (upstreams.isEmpty()) {
      throw new IllegalStateException("No upstream servers available");
    }
    int index = (int) (Math.random() * upstreams.size());
    return upstreams.get(index);
  }

}
