public class SslFactory {
    private static final OssLog LOG = OssLogFactory.getLogger(SslFactory.class);

    /**
     * create ssl context for netty server and client
     */
    public static SslContext createSslContext(SslType sslType) {
        try {
            // ssl.keystore.password, ssl.keystore.location
            String keyStorePwd = get("filelist").get("server.p12").get("storePass").textValue();
            String sslKeyStore = privatekeyFilePath;
            String trustCertCollectionFilePath = AppFilePathUtil.getSslIrTrustCerPath();

            // 创建keyStore
            KeyStore keyStore = createKeyStore(sslKeyStore, "PKCS12", "");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            kmf.init(keyStore, cipher.decryptV2(keyStorePwd.toCharArray());

            // trustManager config trust.cer
            if (sslType == SslType.CLIENT) {
                return SslContextBuilder.forClient()
                    .trustManager(new File(trustCertCollectionFilePath))
                    .keyManager(kmf)
                    .sslProvider(SslContext.defaultServerProvider())
                    .protocols(SslConstants.PROTOCOL_TLS_V1_3, SslConstants.PROTOCOL_TLS_V1_2)
                    .applicationProtocolConfig(
                        // NO_ADVERTISE and ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                            SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
            }
            return SslContextBuilder.forServer(kmf)
                .trustManager(new File(trustCertCollectionFilePath))
                .sslProvider(SslContext.defaultServerProvider())
                .protocols(SslConstants.PROTOCOL_TLS_V1_3, SslConstants.PROTOCOL_TLS_V1_2)
                .applicationProtocolConfig(
                    // NO_ADVERTISE and ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                    new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                        SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .clientAuth(ClientAuth.REQUIRE)
                .build();
        } catch (Exception e) {
            LOG.error("failed to config ssl with exception:", e);
        }
        return null;
    }
}
