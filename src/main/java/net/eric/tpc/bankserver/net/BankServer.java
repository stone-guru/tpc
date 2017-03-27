package net.eric.tpc.bankserver.net;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class BankServer {
    private static final int PORT = 10021;
   
    public static void main(String[] args) throws IOException{
        int port = PORT;
        if(!(args == null || args.length == 0)){
            port = Integer.parseInt(args[0]);
        }
        BankServer s1 = new BankServer();
        s1.startServer(port);
    }
    
    private void startServer(int port) throws IOException{
        IoAcceptor acceptor = new NioSocketAcceptor();

        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
                //new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        
        acceptor.setHandler(BankServiceFactory.newIoHandlerAdapter());
        acceptor.getSessionConfig().setReadBufferSize( 2048 );
        acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, 10 );
        acceptor.bind( new InetSocketAddress(port) );
    }
}
