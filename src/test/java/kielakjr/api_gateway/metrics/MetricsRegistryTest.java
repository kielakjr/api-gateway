package kielakjr.api_gateway.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsRegistryTest {

  private MetricsRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new MetricsRegistry();
  }

  @Test
  void recordRequest_incrementsTotalRequests() {
    registry.recordRequest(10, 200);
    registry.recordRequest(20, 201);

    assertEquals(2, registry.getTotalRequests().getCount());
  }

  @Test
  void recordRequest_200_doesNotIncrementErrorCounters() {
    registry.recordRequest(10, 200);

    assertEquals(0, registry.getServerErrors().getCount());
    assertEquals(0, registry.getClientErrors().getCount());
  }

  @Test
  void recordRequest_4xx_incrementsClientErrors() {
    registry.recordRequest(10, 400);
    registry.recordRequest(10, 404);
    registry.recordRequest(10, 429);

    assertEquals(3, registry.getClientErrors().getCount());
    assertEquals(0, registry.getServerErrors().getCount());
  }

  @Test
  void recordRequest_5xx_incrementsServerErrors() {
    registry.recordRequest(10, 500);
    registry.recordRequest(10, 502);
    registry.recordRequest(10, 503);

    assertEquals(3, registry.getServerErrors().getCount());
    assertEquals(0, registry.getClientErrors().getCount());
  }

  @Test
  void recordRequest_recordsLatency() {
    registry.recordRequest(50, 200);
    registry.recordRequest(100, 200);
    registry.recordRequest(150, 200);

    double p50 = registry.getLatencyHistogram().getPercentile(50);
    assertEquals(100, p50);
  }

  @Test
  void recordRequest_mixedStatusCodes_countsCorrectly() {
    registry.recordRequest(10, 200);
    registry.recordRequest(10, 201);
    registry.recordRequest(10, 400);
    registry.recordRequest(10, 404);
    registry.recordRequest(10, 500);

    assertEquals(5, registry.getTotalRequests().getCount());
    assertEquals(2, registry.getClientErrors().getCount());
    assertEquals(1, registry.getServerErrors().getCount());
  }

  @Test
  void recordRequest_399_notAnError() {
    registry.recordRequest(10, 399);

    assertEquals(0, registry.getClientErrors().getCount());
    assertEquals(0, registry.getServerErrors().getCount());
  }

  @Test
  void toJson_emptyRegistry_returnsValidJson() {
    String json = registry.toJson();

    assertTrue(json.contains("\"totalRequests\": 0"));
    assertTrue(json.contains("\"serverErrors\": 0"));
    assertTrue(json.contains("\"clientErrors\": 0"));
    assertTrue(json.contains("\"p50\": 0.00"));
    assertTrue(json.contains("\"p95\": 0.00"));
    assertTrue(json.contains("\"p99\": 0.00"));
  }

  @Test
  void toJson_withRequests_containsCorrectCounts() {
    registry.recordRequest(10, 200);
    registry.recordRequest(20, 400);
    registry.recordRequest(30, 500);

    String json = registry.toJson();

    assertTrue(json.contains("\"totalRequests\": 3"));
    assertTrue(json.contains("\"serverErrors\": 1"));
    assertTrue(json.contains("\"clientErrors\": 1"));
  }

  @Test
  void toJson_containsLatencyPercentiles() {
    for (int i = 1; i <= 100; i++) {
      registry.recordRequest(i, 200);
    }

    String json = registry.toJson();

    assertTrue(json.contains("\"p50\":"));
    assertTrue(json.contains("\"p95\":"));
    assertTrue(json.contains("\"p99\":"));
  }
}
