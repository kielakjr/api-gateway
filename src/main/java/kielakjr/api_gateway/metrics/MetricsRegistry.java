package kielakjr.api_gateway.metrics;

public class MetricsRegistry {
  private Counter totalRequests = new Counter();
  private Counter serverErrors = new Counter();
  private Counter clientErrors = new Counter();
  private Histogram latencyHistogram = new Histogram();

  public void recordRequest(long latency, int statusCode) {
    totalRequests.increment();
    if (statusCode >= 500) {
      serverErrors.increment();
    } else if (statusCode >= 400) {
      clientErrors.increment();
    }
    latencyHistogram.record(latency);
  }

  public Counter getTotalRequests() {
    return totalRequests;
  }

  public Counter getServerErrors() {
    return serverErrors;
  }

  public Counter getClientErrors() {
    return clientErrors;
  }

  public Histogram getLatencyHistogram() {
    return latencyHistogram;
  }

  public String toJson() {
    return String.format(
        "{\"totalRequests\": %d, \"serverErrors\": %d, \"clientErrors\": %d, \"latency\": {\"p50\": %.2f, \"p95\": %.2f, \"p99\": %.2f}}}",
        totalRequests.getCount(),
        serverErrors.getCount(),
        clientErrors.getCount(),
        latencyHistogram.getPercentile(50),
        latencyHistogram.getPercentile(95),
        latencyHistogram.getPercentile(99)
    );
  }
}
