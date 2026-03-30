package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import kielakjr.api_gateway.context.RequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingFilter implements Filter {
  private final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

  @Override
  public boolean apply(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext rctx) {
    log.info("Incoming request: {}", request.uri());
    log.info("Method: {}", request.method());
    log.info("Request ID: {}", rctx.getRequestId());
    return true;
  }
}
