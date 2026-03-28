package kielakjr.api_gateway.proxy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;


public class ProxyClient {

  public ProxyResponse forwardRequest(String method, String url, byte[] body) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .method(method, HttpRequest.BodyPublishers.ofByteArray(body))
        .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

    return new ProxyResponse(response.statusCode(), response.body());
  }
}

