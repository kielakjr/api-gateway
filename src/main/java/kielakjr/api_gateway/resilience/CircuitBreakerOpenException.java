package kielakjr.api_gateway.resilience;

public class CircuitBreakerOpenException extends RuntimeException {
  public CircuitBreakerOpenException(String message) {
    super(message);
  }
}
