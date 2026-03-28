package kielakjr.api_gateway.loadbalancer;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LoadBalancerStrategy {
  @JsonProperty("round-robin")
  ROUND_ROBIN,
  @JsonProperty("weighted-random")
  WEIGHTED_RANDOM
}
