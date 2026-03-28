package kielakjr.api_gateway.loadbalancer;

import java.util.List;

public interface LoadBalancer {
  String nextUpstream(List<String> upstreams);
}
