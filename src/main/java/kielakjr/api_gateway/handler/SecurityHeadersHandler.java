package kielakjr.api_gateway.handler;

import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import kielakjr.api_gateway.filter.ResponseTransformFilter;

public class SecurityHeadersHandler extends ChannelOutboundHandlerAdapter{
  private final ResponseTransformFilter responseTransformFilter = new ResponseTransformFilter();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof FullHttpResponse response) {
      responseTransformFilter.transform(response);
    }
    super.write(ctx, msg, promise);
  }
}
