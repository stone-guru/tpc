package net.eric.tpc.net;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.net.binary.MessageCodecFactory;
import net.eric.tpc.proto.Types.TransStartRec;

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
        //request.setParam(new ActionStatus(BizCode.ACCOUNT_LOCKED, "ACCOUNT9383873"));
        request.setParam(genTransStartRec());
        request.setContent(createBill("ABC8008"));
        WriteFuture wf = session.write(request);
        wf.awaitUninterruptibly();
        
        ReadFuture rf = session.read();
        rf.awaitUninterruptibly();
        Object response = rf.getMessage();
        System.out.println("Response:" + response.toString());
        
        session.closeNow();
        connector.dispose();
    }
    
    private static TransferBill createBill(String transSn) {
        TransferBill msg = new TransferBill();
        msg.setTransSN(transSn);
        msg.setLaunchTime(new Date());
        msg.setPayer(new AccountIdentity("james", "BOC"));
        msg.setReceiver(new AccountIdentity("lori", "CCB"));
        msg.setReceivingBankCode("ABC");
        msg.setAmount(BigDecimal.valueOf(12.8));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        return msg;
    }
    
    private static TransStartRec genTransStartRec(){
        TransStartRec rec = new TransStartRec();
        rec.setXid(10086);
        rec.setCoordinator(InetSocketAddress.createUnresolved("localhost", 10024));
        try {
            rec.setParticipants(ImmutableList.of(new InetSocketAddress(InetAddress.getLocalHost(), 10021), //
                    new InetSocketAddress(InetAddress.getByName("www.baidu.com"), 80)));
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return rec;
    }
}
