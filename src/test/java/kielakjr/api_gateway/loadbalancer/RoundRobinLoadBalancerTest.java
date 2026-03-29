package kielakjr.api_gateway.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinLoadBalancerTest {

  @Test
  void nextUpstream_cyclesThroughUpstreams() {
    RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
    List<String> upstreams = List.of("http://a", "http://b", "http://c");

    assertEquals("http://a", lb.nextUpstream(upstreams));
    assertEquals("http://b", lb.nextUpstream(upstreams));
    assertEquals("http://c", lb.nextUpstream(upstreams));
    assertEquals("http://a", lb.nextUpstream(upstreams));
  }

  @Test
  void nextUpstream_singleUpstream_alwaysReturnsSame() {
    RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
    List<String> upstreams = List.of("http://only");

    assertEquals("http://only", lb.nextUpstream(upstreams));
    assertEquals("http://only", lb.nextUpstream(upstreams));
  }

  @Test
  void nextUpstream_emptyList_throwsException() {
    RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();

    assertThrows(IllegalStateException.class, () -> lb.nextUpstream(List.of()));
  }
}
