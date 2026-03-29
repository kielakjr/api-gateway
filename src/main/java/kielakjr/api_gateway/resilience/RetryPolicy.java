package kielakjr.api_gateway.resilience;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import kielakjr.api_gateway.proxy.ProxyResponse;

public class RetryPolicy {
  private final int maxRetries;
  private final long initialDelayMs;
  private final double backoffMultiplier;

  public RetryPolicy(int maxRetries, long initialDelayMs, double backoffMultiplier) {
    this.maxRetries = maxRetries;
    this.initialDelayMs = initialDelayMs;
    this.backoffMultiplier = backoffMultiplier;
  }

  public CompletableFuture<ProxyResponse> execute(Supplier<CompletableFuture<ProxyResponse>> action) {
    return executeWithRetry(action, 0, 0);
  }

  public CompletableFuture<ProxyResponse> executeWithRetry(Supplier<CompletableFuture<ProxyResponse>> action, int currentAttempt, long delay) {
    Executor delayedExecutor = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS);
    System.out.println("Attempt: " + currentAttempt + ", delay: " + delay + "ms");
    return CompletableFuture.supplyAsync(() -> null, delayedExecutor)
        .thenCompose(ignored -> action.get())
        .exceptionallyCompose(throwable -> {
          if (currentAttempt < maxRetries) {
            long nextDelay = currentAttempt == 0 ? initialDelayMs : (long) (delay * backoffMultiplier);
            return executeWithRetry(action, currentAttempt + 1, nextDelay);
          }
          return CompletableFuture.failedFuture(throwable);
        });
  }
}
