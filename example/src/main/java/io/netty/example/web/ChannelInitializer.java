import netty.handler.HttpWsHandler;
import netty.handler.http2.Http2HandlerBuilder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

import javax.net.ssl.SSLEngine;

public class ChannelInitializer extends io.netty.channel.ChannelInitializer<SocketChannel> {

    private final SslContext sslContext;
    
    private final boolean startTls;

    /**
     * true if the engine should start its handshaking in "client" mode
     */
    private final boolean isClientMode;

    /**
     * needClientAuth indecates that need client auth also(double direction auth)
     */
    private final boolean needClientAuth;
    
    public ChannelInitializer(SslContext sslContext, boolean clientMode, boolean needClientAuth,
        boolean startTls) {
        this.sslContext = sslContext;
        this.isClientMode = clientMode;
        this.needClientAuth = needClientAuth;
        this.startTls = startTls;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        // HTTP2 or HTTP config for channel
        // configureClearText(ch);

        // code block below can be used for test websocket upgrade
        ChannelPipeline pl = ch.pipeline();
        // ssl config for channel
        if (sslContext != null) {
            SSLEngine engine = sslContext.newEngine(ch.alloc());
            engine.setEnabledProtocols(new String[] {SslConstants.PROTOCOL_TLS_V1_3, SslConstants.PROTOCOL_TLS_V1_2});
            // here as server mode since clientMode is false
            engine.setUseClientMode(isClientMode);
            // need client auth also, same as: SslContextBuilder.clientAuth(ClientAuth.REQUIRE)
            engine.setNeedClientAuth(needClientAuth);
            pl.addFirst(new SslHandler(engine, startTls));
        }

        pl.addLast(new HttpServerCodec());
        pl.addLast(new HttpServerExpectContinueHandler());
        // support for websocket handle
        pl.addLast("http-aggregator", new HttpObjectAggregator(65536));
        pl.addLast(new WebSocketServerCompressionHandler());
        pl.addLast(new WebSocketServerProtocolHandler("/websocket", null, true));
        pl.addLast(new NettyChannelHandler());
    }

    /**
     * config for HTTP2 Or HTTP channel
     *
     * @param ch {@link SocketChannel}
     */
    private void configureClearText(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler
            = new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, new Http2HandlerBuilder().build());
        p.addLast(cleartextHttp2ServerUpgradeHandler);
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.addAfter(ctx.name(), null, new HttpAndWsHandler());
                pipeline.replace(this, null, new HttpObjectAggregator(NettyConstants.MAX_CONTENT_LENGTH));
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                return;

            }
        });
        p.addLast(new UserEventLogger());
    }

    /**
     * UpgradeCodecFactory 是一个接口, 里面定义了一个方法: UpgradeCodec newUpgradeCodec(CharSequence var1)
     * public interface UpgradeCodecFactory {
     * HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence var1);
     * }
     */
    private static final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {
        @Override
        public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            // here only handle protocol is h2c
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new Http2HandlerBuilder().build());
            } else {
                return null;
            }
        }
    };

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            System.out.println("User Event Triggered: " + evt);
            ctx.fireUserEventTriggered(evt);
        }
    }
}
