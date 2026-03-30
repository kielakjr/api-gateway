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
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;

import kielakjr.api_gateway.router.Router;
import kielakjr.api_gateway.filter.FilterChain;
import kielakjr.api_gateway.proxy.ProxyClient;
import kielakjr.api_gateway.resilience.CircuitBreakerOpenException;
import kielakjr.api_gateway.context.RequestContext;
import kielakjr.api_gateway.metrics.MetricsRegistry;
import kielakjr.api_gateway.metrics.MetricsCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final Router router;
  private final FilterChain filterChain;
  private final ProxyClient proxyClient;
  private final Logger log = LoggerFactory.getLogger(GatewayHandler.class);
  private final MetricsCollector metricsCollector;

  public GatewayHandler(Router router, FilterChain filterChain, ProxyClient proxyClient, MetricsRegistry metricsRegistry) {
    this.router = router;
    this.filterChain = filterChain;
    this.proxyClient = proxyClient;
    this.metricsCollector = new MetricsCollector(metricsRegistry);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
    RequestContext rctx = filterChain.execute(ctx, msg);
    if (rctx != null) {
      String route = router.resolve(msg.uri());
      if (route == null) {
        rctx.setStatusCode(404);
        metricsCollector.recordRequest(rctx.getStartTimeNanos(), rctx.getStatusCode());
        writeNotFoundResponse(ctx);
        return;
      }
      proxyClient.forwardRequest(route, msg).thenAccept(response -> {
        writeResponse(ctx, msg, response.getStatusCode(), response.getContentType(), response.getBody() != null ? new String(response.getBody(), CharsetUtil.UTF_8) : null);
        rctx.setResolvedUpstream(route);
        rctx.setMatchedRoute(route);
        rctx.setStatusCode(response.getStatusCode());
      }).exceptionally(throwable -> {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof CircuitBreakerOpenException) {
          writeServiceUnavailableResponse(ctx);
          return null;
        }
        log.error("Error forwarding request to upstream", cause);
        rctx.setStatusCode(502);
        writeBadGatewayResponse(ctx);
        return null;
      })
      .whenComplete((resp, throwable) -> {
          metricsCollector.recordRequest(rctx.getStartTimeNanos(), rctx.getStatusCode());
          msg.release();
        }
      );
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    Throwable rootCause = cause.getCause() != null ? cause.getCause() : cause;
    if (rootCause instanceof ReadTimeoutException) {
      writeGatewayTimeoutResponse(ctx);
    } else {
      log.error("Unexpected error while processing request", cause);
      writeInternalServerErrorResponse(ctx);
    }
  }

  private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, int statusCode, String contentType, String content) {
    boolean keepAlive = HttpUtil.isKeepAlive(request);

    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.valueOf(statusCode),
      Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
    );

    if (content == null) {
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
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

  private void writeNotFoundResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.NOT_FOUND,
      Unpooled.EMPTY_BUFFER
    );
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private void writeBadGatewayResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.BAD_GATEWAY,
      Unpooled.EMPTY_BUFFER
    );
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private void writeServiceUnavailableResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.SERVICE_UNAVAILABLE,
      Unpooled.EMPTY_BUFFER
    );
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private void writeGatewayTimeoutResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.GATEWAY_TIMEOUT,
      Unpooled.EMPTY_BUFFER
    );
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private void writeInternalServerErrorResponse(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.INTERNAL_SERVER_ERROR,
      Unpooled.EMPTY_BUFFER
    );
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }
}
