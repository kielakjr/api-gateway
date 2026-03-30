package kielakjr.api_gateway.filter;

import kielakjr.api_gateway.context.RequestContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.buffer.Unpooled;

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
    }

    rctx.setCorsOrigin(origin);

    if (request.method().name().equals("OPTIONS")) {
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.NO_CONTENT,
          Unpooled.EMPTY_BUFFER
      );
      response.headers().set("Access-Control-Allow-Origin", String.join(", ", corsConfig.getAllowedOrigins()));
      response.headers().set("Access-Control-Allow-Methods", String.join(", ", corsConfig.getAllowedMethods()));
      response.headers().set("Access-Control-Allow-Headers", String.join(", ", corsConfig.getAllowedHeaders()));
      response.headers().set("Access-Control-Max-Age", String.valueOf(corsConfig.getMaxAgeSeconds()));
      response.headers().setInt("Content-Length", 0);
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
      return false;
    }

    return true;
  }
}
