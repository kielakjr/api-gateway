package kielakjr.api_gateway.filter;

import kielakjr.api_gateway.context.RequestContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import kielakjr.api_gateway.config.CorsConfig;

public class CorsFilter implements Filter {
  private final CorsConfig corsConfig;

  public CorsFilter(CorsConfig corsConfig) {
    this.corsConfig = corsConfig;
  }

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    String origin = request.headers().get("Origin");
    if (origin == null) {
      return true;
    } else {
      request.headers().set("Access-Control-Allow-Origin", corsConfig.getAllowedOrigins().contains(origin) ? origin : "*");
      request.headers().set("Access-Control-Allow-Methods", String.join(", ", corsConfig.getAllowedMethods()));
      request.headers().set("Access-Control-Allow-Headers", String.join(", ", corsConfig.getAllowedHeaders()));

      rctx.setCorsOrigin(origin);

      if (request.method().name().equals("OPTIONS")) {
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        return false;
      }

      return true;
    }

  }
}
