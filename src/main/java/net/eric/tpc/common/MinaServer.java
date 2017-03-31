package net.eric.tpc.common;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import net.eric.tpc.common.KeyGenerator.KeyPersister;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.service.CommonServiceFactory;

abstract public class MinaServer {
    private ServerConfig config;
    private IoAcceptor acceptor;

    public MinaServer(ServerConfig config) {
        this.config = config;

    }

    public void start() {
        if (this.startListen()) {
            this.displaySplash();
        } else {
            System.out.println("Unable to start " + config.getBankCode() + " server ");
        }
    }


    public void shutdown() {
        if (acceptor != null && !acceptor.isDisposed()) {
            acceptor.dispose();
        }
    }

    private boolean startListen() {
        acceptor = new NioSocketAcceptor();

        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(this.getCodecFactory()));

        acceptor.setHandler(this.getIoHandler());

        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 2);

        try {
            acceptor.bind(new InetSocketAddress(config.getPort()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected ProtocolCodecFactory getCodecFactory() {
        return new ObjectSerializationCodecFactory();
    }

    protected String getSplashText(String bankCode) {
        return null;
    }

    abstract protected IoHandler getIoHandler();
    

    private void displaySplash() {
        final String msg = String.format("%s server started on port %d \nusing database %s", //
                config.getBankCode(), config.getPort(), config.getDbUrl());
        System.out.println(msg);
        
        String splashText = this.getSplashText(config.getBankCode());
        if (splashText != null) {
            System.out.println();
            System.out.println(splashText);
            System.out.println();
        }
        System.out.println(config.getBankCode() + " server is ready for request");
    }
}
