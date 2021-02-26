
// 实现路由转发
public class HttpDispatcher HttpChannelHandlable {
    @Override
    public void handle(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest request = (FullHttpRequest) msg;
        dispatchRequest(ctx, request);
    }
  public void dispatchRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
      // netty 源码中有对100的返回, 1.0协议  
      if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER));
            LOG.error("ommitted and continue since is100ContinueExpected request:{}", request);
            return;
        }

        // Handle a bad request.
        if (hasHandleBadRequest(ctx, request)) {
            return;
        }

        // 解析路由转发
        Map<HttpLabel, HttpHandlable> map = DefaultHttpRouters.getInstance().getRoutersMap();
        String httpuri = request.uri();
        HttpMethod method = request.method();
        if (httpuri.contains("?")) {
            httpuri = httpuri.substring(0, httpuri.indexOf("?"));
        }

        FullHttpResponse res = new DefaultFullHttpResponse(request.protocolVersion(), OK);
        res.headers().set(CONTENT_TYPE, TEXT_PLAIN);
        HttpHandlable handler = map.get(new HttpLabel(uri, method));

        /**
         * handler not exists inrouters
         **/
        if (handler == null) {
            response.setStatus(NOT_FOUND).headers().setInt(CONTENT_LENGTH, respo.content().readableBytes());
            ctx.write(res).addListener(ChannelFutureListener.CLOSE);
            LOG.error("failed to handle method:{}, uri:{}, handler is null", httpMethod, httpuri);
            return;
        }

        // handle: http2, http, websocket
        handler.handle(ctx, request, response);
        // netty 会解析头部是否为默认
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        LOG.info("succeed to handle method:{}, uri:{}", httpMethod, httpuri);

        // 是否对100, 保留心跳,netty源码是默认支持
        // keep alive for request with keepAlive, also http1.0 since 1.0 default value is close
        res.headers().set(CONNECTION, CLOSE);
        if (keepAlive || (isKeepAlive(request) && request.protocolVersion().equals(HTTP_1_0))) {
            res.headers().set(CONNECTION, KEEP_ALIVE);
        }

        ChannelFuture f = ctx.write(response);
        f.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
}
