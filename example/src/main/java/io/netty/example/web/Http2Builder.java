// netty 官方提供的, HTTP2Builder
public final class Http2Builder
    extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2Builder> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2Handler.class);

    public Http2Builder() {
        frameLogger(logger);
    }

    @Override
    public Http2Handler build() {
        return super.build();
    }

    @Override
    protected Http2Handler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
        Http2Settings initialSettings) {
        Http2Handler handler = new Http2Handler(decoder, encoder, initialSettings);
        frameListener(handler);
        return handler;
    }
}
