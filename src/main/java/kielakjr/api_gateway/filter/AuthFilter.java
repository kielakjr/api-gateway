package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import kielakjr.api_gateway.context.RequestContext;

class JwtUtil {
  public static boolean validateToken(String token, String secret) {
    try {
      SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      System.out.println("Token validation error: " + e.getMessage());
      return false;
    }
  }
}

public class AuthFilter implements Filter {
  private String jwtSecret;

  public AuthFilter(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    String authHeader = request.headers().get("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      rctx.setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
      writeUnauthorizedResponse(ctx);
      return false;
    }

    String token = authHeader.substring(7);
    if (!JwtUtil.validateToken(token, jwtSecret)) {
      rctx.setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
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
