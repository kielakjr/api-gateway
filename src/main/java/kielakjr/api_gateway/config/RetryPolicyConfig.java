package kielakjr.api_gateway.config;

public class RetryPolicyConfig {
  private int maxRetries;
  private long initialDelayMs;
  private double backoffMultiplier;

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public long getInitialDelayMs() {
    return initialDelayMs;
  }

  public void setInitialDelayMs(long initialDelayMs) {
    this.initialDelayMs = initialDelayMs;
  }

  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public void setBackoffMultiplier(double backoffMultiplier) {
    this.backoffMultiplier = backoffMultiplier;
  }
}
