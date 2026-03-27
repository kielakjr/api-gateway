package kielakjr.api_gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface Filter {
  boolean apply(ChannelHandlerContext ctx, FullHttpRequest request);
}
