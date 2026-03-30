package kielakjr.api_gateway.metrics;

public class MetricsRegistry {
  private Counter totalRequests = new Counter();
  private Counter totalErrors = new Counter();
  private Histogram latencyHistogram = new Histogram();

  public void recordRequest(long latency, boolean error) {
    totalRequests.increment();
    if (error) {
      totalErrors.increment();
    }
    latencyHistogram.record(latency);
  }

  public Counter getTotalRequests() {
    return totalRequests;
  }

  public Counter getTotalErrors() {
    return totalErrors;
  }

  public Histogram getLatencyHistogram() {
    return latencyHistogram;
  }

  public String toJson() {
    return String.format(
        "{\"totalRequests\": %d, \"totalErrors\": %d, \"latency\": {\"p50\": %.2f, \"p95\": %.2f, \"p99\": %.2f}}}",
        totalRequests.getCount(),
        totalErrors.getCount(),
        latencyHistogram.getPercentile(50),
        latencyHistogram.getPercentile(95),
        latencyHistogram.getPercentile(99)
    );
  }
}
