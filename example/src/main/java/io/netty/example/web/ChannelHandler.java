package com.huawei.cloudsop.camp.messaging.netty;

import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import HttpAndWsHandler;
import netty.handler.http2.Http2HandlerBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.HttpServerCodec;


/**
 * 会自动协商
**/
public class ChannelHandler extends ApplicationProtocolNegotiationHandler {
    /**
     * websocket path
     */
    private static final String WEBSOCKET_PATH = "/test_websocket_dir";

    /**
     * fallbackProtocol the name of the protocol to use when
     * ALPN/NPN negotiation fails or the client does not support ALPN/NPN
     */
    protected ChannelHandler() {
        super(ApplicationProtocolNames.HTTP_1_1);
    }

    /**
     * When ALPN happens {@link ApplicationProtocolNegotiationHandler} will call this method,
     * and will config certain handler for channel
     */
    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        // pipeline for http2 handler
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(new Http2HandlerBuilder().build());
            return;
        }

        // pipeline for http1 and Websocket over http1.1
        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            ChannelPipeline pl = ctx.pipeline();
            pl.addLast(new HttpServerCodec());
            pl.addLast(new HttpServerExpectContinueHandler());
            pl.addLast("http-aggregator", new HttpObjectAggregator(NettyConstants.MAX_CONTENT_LENGTH));

            // support for websocket handle
            pl.addLast(new WebSocketServerCompressionHandler());
            pl.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));

            // http and websocket handler, dispatch request
            pl.addLast(new HttpAndWsHandler());
            return;
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }
}
