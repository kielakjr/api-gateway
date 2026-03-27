package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

public class AuthFilter implements Filter {
  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request) {
    String authHeader = request.headers().get("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeUnauthorizedResponse(ctx);
      return false;
    }

    String token = authHeader.substring(7);
    if (!"valid-token".equals(token)) {
      writeUnauthorizedResponse(ctx);
      return false;
    }
    return true;
  }

  private void writeUnauthorizedResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.UNAUTHORIZED,
      Unpooled.copiedBuffer("Unauthorized", CharsetUtil.UTF_8)
    );
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

}
