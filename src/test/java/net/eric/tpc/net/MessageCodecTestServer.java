package net.eric.tpc.net;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import net.eric.tpc.net.binary.Message;
import net.eric.tpc.net.binary.MessageCodecFactory;

public class MessageCodecTestServer {
    public static void main(String[] args) {
        IoAcceptor acceptor = new NioSocketAcceptor();

        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MessageCodecFactory()));

        acceptor.setHandler(new IoHandlerAdapter(){
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                System.out.println(message.toString());
                Message request = (Message)message;
                request.setCommandAnswer((short)3);
                session.write(request);
            }
        });

        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 2);

        try {
            acceptor.bind(new InetSocketAddress(10030));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
