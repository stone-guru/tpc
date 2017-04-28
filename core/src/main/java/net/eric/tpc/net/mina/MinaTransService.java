package net.eric.tpc.net.mina;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.eric.tpc.net.RequestHandlers;
import net.eric.tpc.net.binary.MessageCodecFactory;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.proto.PeerTransactionManager;

public class MinaTransService<B> extends AbstractIdleService {
    private static final Logger logger = LoggerFactory.getLogger(MinaTransService.class);

    private NioSocketAcceptor acceptor;
    private IoHandler ioHandler;
    private int port;
    private ProtocolCodecFactory codecFactory;
    private PeerTransactionManager<?> transManager;
    
    @Inject
    public MinaTransService(@Named("Service Port") int port, //
            @Named("Peer Trans Manager") PeerTransactionManager<B> transManager, //
            @Named("Extra Object Codecs") List<ObjectCodec> extraObjectCodecs) {
        this.port = port;
        this.ioHandler = new PeerIoHandler(RequestHandlers.getBasicHandlers(transManager));
        this.codecFactory = new MessageCodecFactory(extraObjectCodecs);
        this.transManager = transManager;
    }

    @Override
    protected void startUp() throws Exception {
        acceptor = new NioSocketAcceptor();

        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(this.codecFactory));

        acceptor.setHandler(this.ioHandler);

        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 2);
        acceptor.bind(new InetSocketAddress(port));

        logger.info("Transaction service startUp");
    }

    @Override
    protected void shutDown() throws Exception {
        if (!acceptor.isDisposing()) {
            acceptor.unbind();
            acceptor.dispose();
            logger.info("Transaction service is shuting down");
        }
        //this.transManager.close();
    }

}
