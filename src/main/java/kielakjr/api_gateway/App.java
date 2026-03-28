package kielakjr.api_gateway;

import kielakjr.api_gateway.config.GatewayConfig;
import kielakjr.api_gateway.server.GatewayServer;
import kielakjr.api_gateway.config.ConfigLoader;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.sun.net.httpserver.HttpServer;

public class App {
  public static void main(String[] args) throws Exception {
    GatewayConfig config = new ConfigLoader().loadConfig("config.yaml");

    Set<Integer> startedPorts = new HashSet<>();
    for (var route : config.getRoutes()) {
      for (String upstream : route.getUpstreams()) {
        int port = URI.create(upstream).getPort();
        if (startedPorts.add(port)) {
          startUpstream(port);
        }
      }
    }

    GatewayServer server = new GatewayServer(config.getServer().getPort(), config.getRoutes(), config.getRateLimitPerMinute());
    server.run();
  }

  private static void startUpstream(int port) throws Exception {
    HttpServer upstream = HttpServer.create(new InetSocketAddress(port), 0);
    upstream.createContext("/", exchange -> {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().toString();
      byte[] requestBody = exchange.getRequestBody().readAllBytes();

      String response = String.format(
          "{\"port\":%d,\"method\":\"%s\",\"path\":\"%s\",\"bodyLength\":%d}",
          port, method, path, requestBody.length
      );

      byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    upstream.start();
  }
}
