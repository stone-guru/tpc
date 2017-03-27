package net.eric.tpc.trans.common;

import static net.eric.tpc.common.Pair.asPair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.eric.tpc.biz.AccountIdentity;
import net.eric.tpc.biz.TransType;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.net.PeerResult2;
import net.eric.tpc.proto.CoorCommuResult;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;

public class TestApp extends MinaCommunicator {

    public static void main(String[] args) throws Exception {

        // TestApp app = new TestApp(new
        // TextLineCodecFactory(Charset.forName("UTF-8")));

        TestApp app = new TestApp(new ObjectSerializationCodecFactory());
        try {
            // app.connectOrAbort(nodes);
            // app.executeHttpCommand();
            app.executeStartTrans();
        } finally {
            app.close();
        }
    }

    public TestApp(ProtocolCodecFactory codecFactory) {
        super(codecFactory);
    }

    private void executeStartTrans() throws Exception {

        Node boc = new Node("localhost", 10021);
        Node ccb = new Node("localhost", 10022);

        // Node abc = new Node("localhost", 10023);
        // Node bdc = new Node("localhost", 10024);

        // super.connectPanticipants(ImmutableList.of(boc, bbc));// , abc,
        // bdc));

        TransferMessage msg = new TransferMessage();
        msg.setTransSN("982872393");
        msg.setTransType(TransType.INCOME);
        msg.setLaunchTime(new Date());
        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(200));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        TransStartRec st = new TransStartRec("KJDF000001", new Node("localhost", 9001), ImmutableList.of(boc, ccb));// ,
                                                                                                                    // abc,
                                                                                                                    // bdc));
        super.connectPanticipants(ImmutableList.of(boc, ccb));
        for (int i = 0; i < 2; i++) {

            Future<CoorCommuResult> result = null;
            try {
                result = this.askBeginTrans(st, ImmutableList.of(asPair(boc, msg), asPair(ccb, msg)));// ,
                                                                                                      // asPair(abc,
                                                                                                      // msg),
                                                                                                      // asPair(bdc,
                                                                                                      // msg)));
            } finally {
                // this.roundRef.get().clearLatch();
            }
            
            @SuppressWarnings("unused")
            CoorCommuResult r = result.get();

        }
        super.closeConnections();
    }

    @SuppressWarnings("unused")
    private void executeHttpCommand() {
        List<Node> nodes = ImmutableList.of(new Node("www.baidu.com", 80), // new
                // Node("locafdsflhost",
                // 80),
                new Node("www.zhihu.com", 80), new Node("221.236.12.130", 80), new Node("www.sina.com", 80),
                new Node("www.cnblogs.com", 80));
        this.connectPanticipants(nodes);

        List<Pair<Node, Object>> requests = this.generateHttpCommand(nodes);
        Future<CoorCommuResult> resultFuture = sendRequest(requests, new HttpHeaderAssembler());
        try {
            CoorCommuResult result = resultFuture.get();
            for (String s : result.okResultAs(TestApp.OBJECT_AS_STRING)) {
                System.out.println(s);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<Pair<Node, Object>> generateHttpCommand(List<Node> nodes) {
        List<Pair<Node, Object>> commands = Lists.newArrayList();
        for (Node node : nodes) {
            Object command = "HEAD /index.html HTTP/1.1\nHOST:" + node.getAddress() + "\n\n";
            commands.add(Pair.asPair(node, command));
        }
        return commands;
    }

    public static class HttpHeaderAssembler implements PeerResult2.Assembler {
        public PeerResult2 start(Node node, Object message) {
            return PeerResult2.pending(node, message.toString());
        }

        public PeerResult2 fold(Node node, PeerResult2 previous, Object message) {
            if (message.toString().length() == 0) {
                return previous.asDone();
            }
            return PeerResult2.pending(node, previous.result().toString() + ", " + message.toString());
        }

        public PeerResult2 finish(Node node, Optional<PeerResult2> previous) {
            if (!previous.isPresent()) {
                return PeerResult2.fail(node, "NO DATA", "NO data received");
            }
            return previous.get().asDone();
        }
    }

}
