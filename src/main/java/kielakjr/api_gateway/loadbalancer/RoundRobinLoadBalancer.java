package kielakjr.api_gateway.loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
  private AtomicInteger counter = new AtomicInteger(0);

  @Override
  public synchronized String nextUpstream(List<String> upstreams) {
    if (upstreams.isEmpty()) {
      throw new IllegalStateException("No upstream servers available");
    }
    String upstream = upstreams.get(counter.getAndIncrement() % upstreams.size());
    return upstream;
  }

}
