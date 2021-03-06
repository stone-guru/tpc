package net.eric.tpc.trans.common;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.mina.filter.codec.textline.TextLineCodecFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.CommunicationRound.WaitType;
import net.eric.tpc.net.MinaChannel;
import net.eric.tpc.net.MinaCommunicator;
import net.eric.tpc.net.PeerResult;
import net.eric.tpc.proto.RoundResult;

public class MinaCommunicatorApp extends MinaCommunicator {

    public static void main(String[] args) throws Exception {
        ExecutorService commuTaskPool = Executors.newCachedThreadPool();
        ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();
        MinaChannel.initSharedConnector(new TextLineCodecFactory(Charset.forName("UTF-8")));
        

        MinaCommunicatorApp app = new MinaCommunicatorApp(commuTaskPool, sequenceTaskPool);
        try {
            // app.connectOrAbort(nodes);
             app.executeHttpCommand();
            //app.executeStartTrans();
        } finally {
            app.close();
        }
        Thread.sleep(1000);
        sequenceTaskPool.shutdown();
        commuTaskPool.shutdown();
        MinaChannel.disposeConnector();
    }

    public MinaCommunicatorApp(ExecutorService commuTaskPool, ExecutorService sequenceTaskPool) {
        super(commuTaskPool, sequenceTaskPool);
    }

//    private void executeStartTrans() throws Exception {
//
//        Node boc = new Node("localhost", 10021);
//        Node ccb = new Node("localhost", 10022);
//
//        // Node abc = new Node("localhost", 10023);
//        // Node bdc = new Node("localhost", 10024);
//
//        // super.connectPanticipants(ImmutableList.of(boc, bbc));// , abc,
//        // bdc));
//
//        TransferBill msg = new TransferBill();
//        msg.setTransSN("982872393");
//        msg.setLaunchTime(new Date());
//        msg.setAccount(new AccountIdentity("mike", "BOC"));
//        msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
//        msg.setReceivingBankCode("BOC");
//        msg.setAmount(BigDecimal.valueOf(200));
//        msg.setSummary("for cigrate");
//        msg.setVoucherNumber("BIK09283-33843");
//
//        TransStartRec st = new TransStartRec("KJDF000001", new Node("localhost", 9001), ImmutableList.of(boc, ccb));// ,
//                                                                                                                    // abc,
//                                                                                                                    // bdc));
//        super.connectPeers(ImmutableList.of(boc, ccb));
//        for (int i = 0; i < 2; i++) {
//
//            Future<RoundResult> result = null;
//            try {
//                result = this.askBeginTrans(st, ImmutableList.of(asPair(boc, msg), asPair(ccb, msg)));// ,
//                                                                                                      // asPair(abc,
//                                                                                                      // msg),
//                                                                                                      // asPair(bdc,
//                                                                                                      // msg)));
//            } finally {
//                // this.roundRef.get().clearLatch();
//            }
//            
//            @SuppressWarnings("unused")
//            RoundResult r = result.get();
//
//        }
//        super.closeConnections();
//    }

    private void executeHttpCommand() {
        List<Node> nodes = ImmutableList.of(new Node("www.baidu.com", 80), // new
                // Node("locafdsflhost",
                // 80),
                new Node("www.zhihu.com", 80), new Node("221.236.12.130", 80), new Node("www.sina.com", 80),
                new Node("www.cnblogs.com", 80));
        this.connectPeers(nodes, WaitType.WAIT_ALL);

        List<Pair<Node, Object>> requests = this.generateHttpCommand(nodes);
        Future<RoundResult> resultFuture = sendRequest(requests, new HttpHeaderAssembler());
        try {
            RoundResult result = resultFuture.get();
            for (String s : result.okResultAs(MinaCommunicator.OBJECT_AS_STRING)) {
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

    public static class HttpHeaderAssembler implements PeerResult.Assembler {
        public PeerResult start(Node node, Object message) {
            return PeerResult.pending(node, message.toString());
        }

        public PeerResult fold(Node node, PeerResult previous, Object message) {
            if (message.toString().length() == 0) {
                return previous.asDone();
            }
            return PeerResult.pending(node, previous.result().toString() + ", " + message.toString());
        }

        public PeerResult finish(Node node, Optional<PeerResult> previous) {
            if (!previous.isPresent()) {
                return PeerResult.fail(node, "NO DATA", "NO data received");
            }
            return previous.get().asDone();
        }
    }

}
