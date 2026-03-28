package kielakjr.api_gateway.proxy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URI;


public class ProxyClient {
  private final HttpClient client = HttpClient.newHttpClient();

  public CompletableFuture<ProxyResponse> forwardRequest(String url, FullHttpRequest msg) {
    ByteBuf content = msg.content();
    byte[] bodyBytes = new byte[content.readableBytes()];
    content.readBytes(bodyBytes);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url + msg.uri()))
        .method(msg.method().name(), HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
        .build();

    CompletableFuture<ProxyResponse> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> new ProxyResponse(response.statusCode(), response.body()));

    return responseFuture;
  }
}

