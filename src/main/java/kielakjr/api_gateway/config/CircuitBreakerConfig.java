package kielakjr.api_gateway.config;

public class CircuitBreakerConfig {
  private int failureThreshold;
  private int recoveryTimeMs;

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public void setFailureThreshold(int failureThreshold) {
    this.failureThreshold = failureThreshold;
  }

  public int getRecoveryTimeMs() {
    return recoveryTimeMs;
  }

  public void setRecoveryTimeMs(int recoveryTimeMs) {
    this.recoveryTimeMs = recoveryTimeMs;
  }
}
