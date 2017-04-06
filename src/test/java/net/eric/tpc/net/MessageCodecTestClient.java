package net.eric.tpc.net;

import java.net.InetSocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.net.binary.MessageCodecFactory;


public class MessageCodecTestClient {
    public static void main(String[] args) {
        SocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(3000);
        DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
        filterChain.addLast("codec", new ProtocolCodecFilter(new MessageCodecFactory()));
        connector.getSessionConfig().setUseReadOperation(true);
        
        ConnectFuture future = connector.connect(new InetSocketAddress("localhost", 10030));
        future.awaitUninterruptibly();
        if (!future.isConnected()) {
            System.out.println("Can not connect");
        }
        IoSession session = future.getSession();
        
        Message request = new Message();
        request.setRound((short)7);
        request.setParam(new ActionStatus(BizCode.ACCOUNT_LOCKED, "ACCOUNT9383873"));
        WriteFuture wf = session.write(request);
        wf.awaitUninterruptibly();
        
        ReadFuture rf = session.read();
        rf.awaitUninterruptibly();
        Object response = rf.getMessage();
        System.out.println("Response:" + response.toString());
        
        session.closeNow();
        connector.dispose();
    }
}
