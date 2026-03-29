package kielakjr.api_gateway.proxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.config.CircuitBreakerConfig;
import kielakjr.api_gateway.config.ConnectionPoolConfig;
import kielakjr.api_gateway.resilience.CircuitBreaker;
import kielakjr.api_gateway.resilience.CircuitBreakerOpenException;
import kielakjr.api_gateway.resilience.RetryPolicy;
import kielakjr.api_gateway.config.RetryPolicyConfig;

public class ProxyClient {
  private HttpClient client;
  private int requestTimeoutSeconds;
  private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
  private final CircuitBreakerConfig circuitBreakerConfig;
  private final RetryPolicy retryPolicy;

  public ProxyClient(ConnectionPoolConfig config, CircuitBreakerConfig cbConfig, RetryPolicyConfig retryConfig) {
    this.client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
        .build();
    this.requestTimeoutSeconds = config.getRequestTimeoutSeconds();
    this.retryPolicy = new RetryPolicy(retryConfig.getMaxRetries(), retryConfig.getInitialDelayMs(), retryConfig.getBackoffMultiplier());
    this.circuitBreakerConfig = cbConfig;
  }

  public CompletableFuture<ProxyResponse> forwardRequest(String url, FullHttpRequest msg) {
    CircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(url, k -> new CircuitBreaker(circuitBreakerConfig.getFailureThreshold(), circuitBreakerConfig.getRecoveryTimeMs()));
    if (!circuitBreaker.allowRequest()) {
      return CompletableFuture.failedFuture(new CircuitBreakerOpenException("Circuit breaker is open"));
    }

    ByteBuf content = msg.content();
    byte[] bodyBytes = new byte[content.readableBytes()];
    content.readBytes(bodyBytes);
    HttpRequest.Builder requestBuild = HttpRequest.newBuilder()
        .uri(URI.create(url + msg.uri()))
        .method(msg.method().name(), HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
        .timeout(Duration.ofSeconds(requestTimeoutSeconds));

    msg.headers().forEach(header -> {
      String name = header.getKey();
      String value = header.getValue();

      if (!name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("Content-Length") && !name.equalsIgnoreCase("Transfer-Encoding") && !name.equalsIgnoreCase("Content-Type") && !name.equalsIgnoreCase("Connection")) {
        requestBuild.header(name, value);
      }
    });
    String contentType = msg.headers().get("Content-Type");
    if (contentType != null) {
      requestBuild.header("Content-Type", contentType);
    }
    HttpRequest request = requestBuild.build();

    String method = msg.method().name();
    boolean isIdempotent = method.equals("GET") || method.equals("HEAD") || method.equals("PUT") || method.equals("DELETE") || method.equals("OPTIONS") || method.equals("TRACE");

    if (isIdempotent) {
      return sendRequest(request, circuitBreaker);
    } else {
      return retryPolicy.execute(() -> sendRequest(request, circuitBreaker));
    }

  }

  private CompletableFuture<ProxyResponse> sendRequest(HttpRequest request, CircuitBreaker circuitBreaker) {

    CompletableFuture<ProxyResponse> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                                                            .thenApply(response -> {
                                                              if (response.statusCode() >= 500) {
                                                                circuitBreaker.recordFailure();
                                                              } else {
                                                                circuitBreaker.recordSuccess();
                                                              }
                                                              return new ProxyResponse(response.statusCode(), response.body(), response.headers().firstValue("Content-Type").orElse("application/json"));
                                                            })
                                                            .exceptionally(throwable -> {
                                                              circuitBreaker.recordFailure();
                                                              throw new RuntimeException(throwable);
                                                            });

    return responseFuture;
  }

}
