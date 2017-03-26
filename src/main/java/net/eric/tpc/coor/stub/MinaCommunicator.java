package net.eric.tpc.coor.stub;

import static net.eric.tpc.common.Pair.asPair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.filter.codec.ProtocolCodecFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.net.PeerResult2;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.VoteResult;

public class MinaCommunicator implements Communicator<TransferMessage> {

    public static final Function<Object, MinaChannel> OBJECT_AS_CHANNEL = new Function<Object, MinaChannel>() {
        public MinaChannel apply(Object obj) {
            return (MinaChannel) obj;
        }
    };

    public static final Function<Object, String> OBJECT_AS_STRING = new Function<Object, String>() {
        public String apply(Object obj) {
            return (String) obj;
        }
    };

    protected Map<Node, MinaChannel> channelMap = Collections.emptyMap();
    protected ExecutorService pool = Executors.newCachedThreadPool();
    protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);
    private  ProtocolCodecFactory codecFactory;
    
    public MinaCommunicator(ProtocolCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @Override
    public boolean connectPanticipants(List<Node> nodes) {
        Optional<Map<Node, MinaChannel>> opt = this.connectOrAbort(nodes);
        if (!opt.isPresent()) {
            System.out.println("Not all node connected");
            return false;
        }
        this.channelMap = opt.get();
        return true;
    }

    @Override
    public Future<CommuResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, TransferMessage>> tasks) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Pair<Node, TransferMessage> p : tasks) {
            final String param1 = DataPacketCodec.encodeTransStartRec(transStartRec);
            final String param2 = DataPacketCodec.encodeTransferMessage(p.snd());
            DataPacket packet = new DataPacket(DataPacket.BEGIN_TRANS, param1, param2);
            final Object msg = DataPacketCodec.encodeDataPacket(packet);
            requests.add(asPair(p.fst(), msg));
        }
        return this.sendRequest(requests, MinaCommunicator.BEGIN_TRANS_ASSEMBLER);
    }

    private static final PeerResult2.Assembler BEGIN_TRANS_ASSEMBLER = new PeerResult2.OneItemAssembler() {
        @Override
        public PeerResult2 start(Node node, Object message) {
            DataPacket packet = DataPacketCodec.decodeDataPacket(message.toString());
            if (DataPacket.BEGIN_TRANS_ANSWER.equals(packet.getCode())) {
                if (DataPacket.YES.equals(packet.getParam1())) {
                    return PeerResult2.approve(node, true);
                } else if (DataPacket.NO.equals(packet.getParam1())) {
                    return PeerResult2.refuse(node, BizErrorCode.REFUSE_TRANS, packet.getParam2());
                }
            }

            return PeerResult2.fail(node, BizErrorCode.PEER_PRTC_ERROR, packet.toString());
        }
    };

    public Future<VoteResult> gatherVote(String xid, List<Node> nodes) {
        return null;
    }

    public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
    }

    public void shutdown() {
        this.closeChannels(this.channelMap.values());
        this.pool.shutdown();
    }

    private void closeChannels(Iterable<MinaChannel> channels) {
        for (final MinaChannel channel : channels) {
            Runnable closeTask = new Runnable() {
                public void run() {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 没啥可做的动作了
                    }
                }
            };
            pool.submit(closeTask);
        }
    }

    public Future<CommuResult> sendRequest(List<Pair<Node, Object>> requests) {
        return this.sendRequest(requests, PeerResult2.ONE_ITEM_ASSEMBLER);
    }

    public Future<CommuResult> sendRequest(List<Pair<Node, Object>> requests, PeerResult2.Assembler assembler) {
        return this.communicate(requests, RoundType.DOUBLE_SIDE, assembler);
    }

    private Future<CommuResult> sendMessage(List<Pair<Node, Object>> messages) {
        return this.communicate(messages, RoundType.SINGLE_SIDE, PeerResult2.ONE_ITEM_ASSEMBLER);
    }

    private Future<CommuResult> communicate(List<Pair<Node, Object>> messages, final RoundType roundType,
            PeerResult2.Assembler assembler) {
        for (final Pair<Node, Object> p : messages) {
            if (!this.channelMap.containsKey(p.fst())) {
                throw new IllegalStateException("Given node not connected first " + p.fst());
            }
        }
        final CommunicationRound round = this.newRound(messages.size(), roundType, assembler);

        for (final Pair<Node, Object> p : messages) {
            final MinaChannel channel = this.channelMap.get(p.fst());
            Runnable task = new Runnable() {
                public void run() {
                    System.out.println("before call channel send");
                    final boolean sendOnly = roundType == RoundType.SINGLE_SIDE;
                    channel.sendMessage(p.snd(), sendOnly);
                    System.out.println("after call channel send");
                }
            };
            this.pool.submit(task);
        }
        return round.getResult();
    }

    private Future<CommuResult> connectAll(List<Node> nodes) {
        final CommunicationRound round = this.newRound(nodes.size(), RoundType.SINGLE_SIDE,
                PeerResult2.ONE_ITEM_ASSEMBLER);

        for (final Node node : nodes) {
            Runnable task = new Runnable() {
                public void run() {
                    MinaChannel channel = new MinaChannel(node, MinaCommunicator.this.codecFactory, MinaCommunicator.this.roundRef);
                    try {
                        channel.connect();
                        round.regMessage(node, channel);
                        System.out.println("connec " + node + " finish reg");
                    } catch (Exception e) {
                        round.regFailure(node, "EXCEPTION", e.getMessage());
                    }
                }
            };
            pool.submit(task);
        }

        return round.getResult();
    }

    private CommunicationRound newRound(int peerCount, RoundType roundType, PeerResult2.Assembler assembler) {
        CommunicationRound round = new CommunicationRound(this.pool, peerCount, roundType, assembler);
        this.roundRef.set(round);
        return round;
    }

    public void closeConnections() {
        // TODO Auto-generated method stub

    }

    private Optional<Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes) {
        Future<CommuResult> future = connectAll(nodes);
        CommuResult result;
        try {
            result = future.get();
            // this.displayConnectResult(result);
            System.out.println("Got async connect Future");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Optional.absent();
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Optional.absent();
        }
        if (result.isAllOK()) {
            Map<Node, MinaChannel> map = new HashMap<Node, MinaChannel>();
            for (PeerResult2 r : result.getResults()) {
                map.put(r.peer(), (MinaChannel) r.result());
            }
            return Optional.of(map);
        }

        closeChannels(result.okResultAs(MinaCommunicator.OBJECT_AS_CHANNEL));
        return Optional.absent();
    }
}
