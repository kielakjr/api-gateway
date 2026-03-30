package kielakjr.api_gateway.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {

  @Test
  void recordRequest_incrementsTotalRequests() {
    MetricsRegistry registry = new MetricsRegistry();
    MetricsCollector collector = new MetricsCollector(registry);

    collector.recordRequest(System.nanoTime() - 5_000_000, 200);

    assertEquals(1, registry.getTotalRequests().getCount());
  }

  @Test
  void recordRequest_4xx_incrementsClientErrors() {
    MetricsRegistry registry = new MetricsRegistry();
    MetricsCollector collector = new MetricsCollector(registry);

    collector.recordRequest(System.nanoTime() - 1_000_000, 404);

    assertEquals(1, registry.getClientErrors().getCount());
  }

  @Test
  void recordRequest_5xx_incrementsServerErrors() {
    MetricsRegistry registry = new MetricsRegistry();
    MetricsCollector collector = new MetricsCollector(registry);

    collector.recordRequest(System.nanoTime() - 1_000_000, 500);

    assertEquals(1, registry.getServerErrors().getCount());
  }

  @Test
  void recordRequest_recordsLatencyInMilliseconds() {
    MetricsRegistry registry = new MetricsRegistry();
    MetricsCollector collector = new MetricsCollector(registry);

    long startTime = System.nanoTime() - 50_000_000;
    collector.recordRequest(startTime, 200);

    double p50 = registry.getLatencyHistogram().getPercentile(50);
    assertTrue(p50 >= 40 && p50 <= 100, "Latency should be approximately 50ms, was: " + p50);
  }

  @Test
  void recordRequest_multipleRequests_allRecorded() {
    MetricsRegistry registry = new MetricsRegistry();
    MetricsCollector collector = new MetricsCollector(registry);

    collector.recordRequest(System.nanoTime(), 200);
    collector.recordRequest(System.nanoTime(), 201);
    collector.recordRequest(System.nanoTime(), 500);

    assertEquals(3, registry.getTotalRequests().getCount());
    assertEquals(1, registry.getServerErrors().getCount());
  }
}
