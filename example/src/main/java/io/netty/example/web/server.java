
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootStrap = new ServerBootstrap();
            bootStrap.group(bossGroup, workerGroup).option(ChannelOption.SO_BACKLOG, SO_BACKLOG)
                .channel(NioServerSocketChannel.class)
                // TODO: remove it before production env
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new NettyChannelInitializer(sslContext, false, true, true));
            Channel ch = bootStrap.bind("127.0.0.1", 8080).sync().channel();
            LOG.info("succeed to start http server and now server is listening");
            ch.closeFuture().sync();
        } finally {
            LOG.warn("http server shutdown gracefully");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    // 重载initconfig
    public void init(){
    }

    // 重载lifecycle
    public void sslConfig() {
        sslContext = SslContextFactory.createSslContext(SslType.SERVER);
        if (sslContext == null) {
            LOG.error("failed to config ssl for http server since sslContext is null");
            return;
        }
        LOG.info("succeed to config ssl for http server");
    }
