
// 实现路由转发
public class HttpDispatcher HttpChannelHandlable {
    @Override
    public void handle(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest request = (FullHttpRequest) msg;
        dispatchRequest(ctx, request);
    }
  public void dispatchRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER));
            LOG.error("ommitted and continue since is100ContinueExpected request:{}", request);
            return;
        }

        // Handle a bad request.
        if (hasHandleBadRequest(ctx, request)) {
            return;
        }

        // parse url path and router from routersMap, delivery request
        Map<HttpLabel, HttpHandlable> map = DefaultHttpRouters.getInstance().getRoutersMap();
        String uri = request.uri();
        HttpMethod method = request.method();
        if (uri.contains("?")) {
            uri = uri.substring(0, uri.indexOf("?"));
        }

        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), OK);
        response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
        HttpHandlable handler = map.get(new HttpLabel(uri, method));

        /**
         * method not found, handler not exists in
         **/
        if (handler == null) {
            response.setStatus(NOT_FOUND).headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            LOG.error("failed to handle method:{}, uri:{}, handler is null", method, uri);
            return;
        }

        // handle request support: http2, http, websocket
        handler.handle(ctx, request, response);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        LOG.info("succeed to handle method:{}, uri:{}", method, uri);

        // keep alive for keepAlive request, also for http1.0 since 1.0 default value is close
        response.headers().set(CONNECTION, CLOSE);
        if (keepAlive || (isKeepAlive(request) && request.protocolVersion().equals(HTTP_1_0))) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }

        ChannelFuture future = ctx.write(response);
        future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
}
