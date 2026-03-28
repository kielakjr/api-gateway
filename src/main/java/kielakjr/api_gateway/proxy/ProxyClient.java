package kielakjr.api_gateway.proxy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.config.ConnectionPoolConfig;

import java.net.URI;


public class ProxyClient {
  private HttpClient client;
  private int requestTimeoutSeconds;

  public ProxyClient(ConnectionPoolConfig config) {
    this.client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
        .build();
    this.requestTimeoutSeconds = config.getRequestTimeoutSeconds();
  }

  public CompletableFuture<ProxyResponse> forwardRequest(String url, FullHttpRequest msg) {
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

    CompletableFuture<ProxyResponse> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> new ProxyResponse(response.statusCode(), response.body(), response.headers().firstValue("Content-Type").orElse("application/json")));

    return responseFuture;
  }
}

