package kielakjr.api_gateway.handler;

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

import kielakjr.api_gateway.router.Router;
import kielakjr.api_gateway.filter.FilterChain;

public class GatewayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private Router router;
  private FilterChain filterChain;

  public GatewayHandler(Router router, FilterChain filterChain) {
    this.router = router;
    this.filterChain = filterChain;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
    if (filterChain.execute(ctx, msg)) {
      writeResponse(ctx, msg, router.resolve(msg.uri()));
    }
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
      content == null ? HttpResponseStatus.NOT_FOUND : HttpResponseStatus.OK,
      content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
    );

    if (content == null) {
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

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
