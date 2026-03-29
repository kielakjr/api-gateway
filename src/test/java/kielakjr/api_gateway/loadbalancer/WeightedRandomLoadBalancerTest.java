package kielakjr.api_gateway.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeightedRandomLoadBalancerTest {

  @Test
  void nextUpstream_returnsValidUpstream() {
    WeightedRandomLoadBalancer lb = new WeightedRandomLoadBalancer();
    List<String> upstreams = List.of("http://a", "http://b", "http://c");

    String result = lb.nextUpstream(upstreams);

    assertTrue(upstreams.contains(result));
  }

  @Test
  void nextUpstream_singleUpstream_returnsThatUpstream() {
    WeightedRandomLoadBalancer lb = new WeightedRandomLoadBalancer();

    assertEquals("http://only", lb.nextUpstream(List.of("http://only")));
  }

  @Test
  void nextUpstream_emptyList_throwsException() {
    WeightedRandomLoadBalancer lb = new WeightedRandomLoadBalancer();

    assertThrows(IllegalStateException.class, () -> lb.nextUpstream(List.of()));
  }

  @Test
  void nextUpstream_distributesAcrossUpstreams() {
    WeightedRandomLoadBalancer lb = new WeightedRandomLoadBalancer();
    List<String> upstreams = List.of("http://a", "http://b", "http://c");
    Set<String> seen = new HashSet<>();

    for (int i = 0; i < 100; i++) {
      seen.add(lb.nextUpstream(upstreams));
    }

    assertEquals(3, seen.size(), "Should eventually select all upstreams");
  }
}
