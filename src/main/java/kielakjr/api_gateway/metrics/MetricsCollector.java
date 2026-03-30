package kielakjr.api_gateway.metrics;

  public class MetricsCollector {
    private final MetricsRegistry metricsRegistry;

    public MetricsCollector(MetricsRegistry metricsRegistry) {
      this.metricsRegistry = metricsRegistry;
    }

    public void recordRequest(long startTimeNanos, int statusCode) {
      long latencyMs = (System.nanoTime() - startTimeNanos) / 1_000_000;
      metricsRegistry.recordRequest(latencyMs, statusCode);
    }
  }
