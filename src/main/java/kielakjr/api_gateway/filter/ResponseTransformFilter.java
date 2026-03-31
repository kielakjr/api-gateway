package kielakjr.api_gateway.filter;

import io.netty.handler.codec.http.FullHttpResponse;

public class ResponseTransformFilter {
  public void transform(FullHttpResponse response) {
    response.headers().set("X-Content-Type-Options", "nosniff");
    response.headers().set("X-Frame-Options", "DENY");
    response.headers().set("Strict-Transport-Security", "max-age=31536000");
  }
}
