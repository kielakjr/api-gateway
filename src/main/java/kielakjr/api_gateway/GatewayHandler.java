package kielakjr.api_gateway;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class GatewayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
    writeResponse(ctx, msg, "Hello, World!");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, String content) {
    boolean keepAlive = HttpUtil.isKeepAlive(request);

    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.OK,
      Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
    );

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

    if (keepAlive) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    if (keepAlive) {
      ctx.writeAndFlush(response);
    } else {
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
