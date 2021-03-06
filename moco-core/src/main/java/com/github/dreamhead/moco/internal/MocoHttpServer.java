package com.github.dreamhead.moco.internal;

import com.github.dreamhead.moco.HttpsCertificate;
import com.github.dreamhead.moco.Runner;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

public class MocoHttpServer extends Runner {
    private final MocoServer server = new MocoServer();
    private final ActualHttpServer serverSetting;

    public MocoHttpServer(ActualHttpServer serverSetting) {
        this.serverSetting = serverSetting;
    }

    public void start() {
        int port = server.start(serverSetting.getPort().or(0), new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (serverSetting.isSecure()) {
                    pipeline.addLast("ssl", sslHandler().get());
                }

                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("handler", new MocoHandler(serverSetting));
            }
        });
        serverSetting.setPort(port);
    }

    private Optional<SslHandler> sslHandler() {
        return serverSetting.getCertificate().transform(toSslHandler());
    }

    private Function<HttpsCertificate, SslHandler> toSslHandler() {
        return new Function<HttpsCertificate, SslHandler>() {
            @Override
            public SslHandler apply(HttpsCertificate certificate) {
                SSLEngine sslEngine = certificate.createSSLEngine();
                sslEngine.setUseClientMode(false);
                return new SslHandler(sslEngine);
            }
        };
    }

    public void stop() {
        server.stop();
    }
}
