package kielakjr.api_gateway.resilience;

import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {
  private enum State {
    CLOSED, OPEN, HALF_OPEN
  }
  private volatile State state = State.CLOSED;
  private AtomicInteger failureCount = new AtomicInteger(0);
  private volatile long openedAt = 0;
  private final int failureThreshold;
  private final int recoveryTimeMs;

  public CircuitBreaker(int failureThreshold, int recoveryTimeMs) {
    this.failureThreshold = failureThreshold;
    this.recoveryTimeMs = recoveryTimeMs;
  }


  public boolean allowRequest() {
    if (state == State.OPEN) {
      if (System.currentTimeMillis() - openedAt > recoveryTimeMs) {
        state = State.HALF_OPEN;
        return true;
      }
      return false;
    }
    return true;
  }

  public void recordSuccess() {
    if (state == State.HALF_OPEN) {
      state = State.CLOSED;
      failureCount.set(0);
    }
    else if (state == State.CLOSED) {
      failureCount.set(0);
    }
  }

  public void recordFailure() {
    if (state == State.HALF_OPEN) {
      state = State.OPEN;
      openedAt = System.currentTimeMillis();
    } else if (state == State.CLOSED) {
      if (failureCount.incrementAndGet() >= failureThreshold) {
        state = State.OPEN;
        openedAt = System.currentTimeMillis();
      }
    }
  }
}
