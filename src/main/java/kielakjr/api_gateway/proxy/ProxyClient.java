package kielakjr.api_gateway.proxy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URI;


public class ProxyClient {
  private final HttpClient client = HttpClient.newHttpClient();

  public ProxyResponse forwardRequest(String url, FullHttpRequest msg) throws Exception {
    ByteBuf content = msg.content();
    byte[] bodyBytes = new byte[content.readableBytes()];
    content.readBytes(bodyBytes);
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url + msg.uri()))
        .method(msg.method().name(), HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
        .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

    return new ProxyResponse(response.statusCode(), response.body());
  }
}

