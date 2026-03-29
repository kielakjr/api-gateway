package kielakjr.api_gateway.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

  @Test
  void allowRequest_initialState_returnsTrue() {
    CircuitBreaker cb = new CircuitBreaker(3, 1000);

    assertTrue(cb.allowRequest());
  }

  @Test
  void allowRequest_belowThreshold_returnsTrue() {
    CircuitBreaker cb = new CircuitBreaker(3, 1000);

    cb.recordFailure();
    cb.recordFailure();

    assertTrue(cb.allowRequest());
  }

  @Test
  void allowRequest_atThreshold_returnsFalse() {
    CircuitBreaker cb = new CircuitBreaker(3, 1000);

    cb.recordFailure();
    cb.recordFailure();
    cb.recordFailure();

    assertFalse(cb.allowRequest());
  }

  @Test
  void allowRequest_afterRecoveryTime_returnsTrue() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(1, 50);

    cb.recordFailure();
    assertFalse(cb.allowRequest());

    Thread.sleep(60);

    assertTrue(cb.allowRequest());
  }

  @Test
  void recordSuccess_afterHalfOpen_closesCirsuit() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(1, 50);

    cb.recordFailure();
    Thread.sleep(60);
    cb.allowRequest();

    cb.recordSuccess();

    assertTrue(cb.allowRequest());
  }

  @Test
  void recordFailure_inHalfOpen_opensCircuit() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(1, 50);

    cb.recordFailure();
    Thread.sleep(60);
    cb.allowRequest();

    cb.recordFailure();

    assertFalse(cb.allowRequest());
  }

  @Test
  void recordSuccess_inClosed_resetsFailureCount() {
    CircuitBreaker cb = new CircuitBreaker(3, 1000);

    cb.recordFailure();
    cb.recordFailure();
    cb.recordSuccess();

    cb.recordFailure();
    cb.recordFailure();
    assertTrue(cb.allowRequest());
  }

  @Test
  void allowRequest_openState_beforeRecoveryTime_returnsFalse() {
    CircuitBreaker cb = new CircuitBreaker(1, 60000);

    cb.recordFailure();

    assertFalse(cb.allowRequest());
    assertFalse(cb.allowRequest());
  }
}
