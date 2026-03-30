package kielakjr.api_gateway.resilience;

import kielakjr.api_gateway.proxy.ProxyResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

  @Test
  void execute_successfulAction_returnsImmediately() throws Exception {
    RetryPolicy policy = new RetryPolicy(3, 100, 2.0);
    ProxyResponse expected = new ProxyResponse(200, "ok".getBytes(), "text/plain");

    CompletableFuture<ProxyResponse> result = policy.execute(
        () -> CompletableFuture.completedFuture(expected));

    ProxyResponse response = result.get();
    assertEquals(200, response.getStatusCode());
  }

  @Test
  void execute_failsThenSucceeds_retriesAndReturns() throws Exception {
    RetryPolicy policy = new RetryPolicy(3, 10, 2.0);
    AtomicInteger attempts = new AtomicInteger(0);

    CompletableFuture<ProxyResponse> result = policy.execute(() -> {
      if (attempts.incrementAndGet() < 3) {
        return CompletableFuture.failedFuture(new RuntimeException("transient error"));
      }
      return CompletableFuture.completedFuture(new ProxyResponse(200, "ok".getBytes(), "text/plain"));
    });

    ProxyResponse response = result.get();
    assertEquals(200, response.getStatusCode());
    assertEquals(3, attempts.get());
  }

  @Test
  void execute_allAttemptsFail_throwsException() {
    RetryPolicy policy = new RetryPolicy(2, 10, 2.0);
    AtomicInteger attempts = new AtomicInteger(0);

    CompletableFuture<ProxyResponse> result = policy.execute(() -> {
      attempts.incrementAndGet();
      return CompletableFuture.failedFuture(new RuntimeException("permanent error"));
    });

    ExecutionException ex = assertThrows(ExecutionException.class, result::get);
    assertInstanceOf(RuntimeException.class, ex.getCause());
    assertEquals(3, attempts.get()); // initial + 2 retries
  }

  @Test
  void execute_zeroRetries_failsAfterFirstAttempt() {
    RetryPolicy policy = new RetryPolicy(0, 100, 2.0);
    AtomicInteger attempts = new AtomicInteger(0);

    CompletableFuture<ProxyResponse> result = policy.execute(() -> {
      attempts.incrementAndGet();
      return CompletableFuture.failedFuture(new RuntimeException("fail"));
    });

    assertThrows(ExecutionException.class, result::get);
    assertEquals(1, attempts.get());
  }

  @Test
  void execute_retriesCorrectNumberOfTimes() throws Exception {
    RetryPolicy policy = new RetryPolicy(5, 10, 1.0);
    AtomicInteger attempts = new AtomicInteger(0);

    CompletableFuture<ProxyResponse> result = policy.execute(() -> {
      if (attempts.incrementAndGet() <= 5) {
        return CompletableFuture.failedFuture(new RuntimeException("fail"));
      }
      return CompletableFuture.completedFuture(new ProxyResponse(200, null, null));
    });

    ProxyResponse response = result.get();
    assertEquals(200, response.getStatusCode());
    assertEquals(6, attempts.get()); // initial + 5 retries
  }

  @Test
  void execute_successOnFirstRetry_stopsRetrying() throws Exception {
    RetryPolicy policy = new RetryPolicy(3, 10, 2.0);
    AtomicInteger attempts = new AtomicInteger(0);

    CompletableFuture<ProxyResponse> result = policy.execute(() -> {
      if (attempts.incrementAndGet() == 1) {
        return CompletableFuture.failedFuture(new RuntimeException("transient"));
      }
      return CompletableFuture.completedFuture(new ProxyResponse(200, null, null));
    });

    ProxyResponse response = result.get();
    assertEquals(200, response.getStatusCode());
    assertEquals(2, attempts.get());
  }

  @Test
  void execute_maxRetriesExceeded_preservesOriginalException() {
    RetryPolicy policy = new RetryPolicy(1, 10, 2.0);

    CompletableFuture<ProxyResponse> result = policy.execute(
        () -> CompletableFuture.failedFuture(new RuntimeException("specific error")));

    ExecutionException ex = assertThrows(ExecutionException.class, result::get);
    assertTrue(ex.getCause().getMessage().contains("specific error"));
  }
}
