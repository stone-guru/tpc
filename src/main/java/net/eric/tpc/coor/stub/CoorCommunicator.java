package net.eric.tpc.coor.stub;

import static net.eric.tpc.base.Pair.asPair;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.CommunicationRound.WaitType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.MinaCommunicator;
import net.eric.tpc.net.PeerResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.TransStartRec;

public class CoorCommunicator extends MinaCommunicator implements Communicator<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(CoorCommunicator.class);

    public CoorCommunicator(ExecutorService commuTaskPool, ExecutorService sequenceTaskPool) {
        super(commuTaskPool, sequenceTaskPool);
    }

    @Override
    public ActionStatus connectPanticipants(List<Node> nodes) {
        return super.connectPeers(nodes, WaitType.WAIT_ALL);
    }

    @Override
    public Future<RoundResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, TransferBill>> tasks) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Pair<Node, TransferBill> p : tasks) {
            final DataPacket packet = new DataPacket(DataPacket.BEGIN_TRANS, transStartRec.getXid(), transStartRec,
                    p.snd());
            requests.add(asPair(p.fst(), (Object) packet));
        }

        return this.sendRequest(requests,
                new YesOrNoAssembler(DataPacket.BEGIN_TRANS_ANSWER, BizCode.REFUSE_TRANS, transStartRec.getXid()));
    }

    @Override
    public Future<RoundResult> gatherVote(String xid, List<Node> nodes) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Node node : nodes) {
            final DataPacket packet = new DataPacket(DataPacket.VOTE_REQ, xid, "");
            requests.add(asPair(node, (Object) packet));
        }

        return this.sendRequest(requests, //
                new YesOrNoAssembler(DataPacket.VOTE_ANSWER, BizCode.REFUSE_COMMIT, xid));
    }

    @Override
    public Future<RoundResult> notifyDecision(String xid, Decision decision, List<Node> nodes) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();

        for (Node node : nodes) {
            if (logger.isDebugEnabled()) {
                logger.debug("Send " + decision + " to " + node);
            }
            String code = null;
            if (decision == Decision.COMMIT) {
                code = DataPacket.YES;
            } else if (decision == Decision.ABORT) {
                code = DataPacket.NO;
            } else {
                throw new UnImplementedException();
            }

            final DataPacket packet = new DataPacket(DataPacket.TRANS_DECISION, xid, code);
            requests.add(asPair(node, (Object) packet));
        }

        return this.sendMessage(requests);
    }

    /**
     * TPC 事务中的通讯结果解码类。执行如下基本检查
     * <ul>
     * <li>底层传递上来的是否是DataPacket类型</li>
     * <li>此DataPacket的code是否为希望的code, (code在构造函数中指定)</li>
     * <li>此DataPacket的param1是否为希望的事务号xid(xid在构造函数中指定</li>
     * </ul>
     * 若满足上述检查，就调用{@link DataPacketAssembler#processDataPacket}来进行进一步的处理，子类须实现此方法。
     *
     */
    private static abstract class DataPacketAssembler extends PeerResult.OneItemAssembler {
        private String code;
        private String xid;

        public DataPacketAssembler(String code, String xid) {
            this.code = code;
            this.xid = xid;
        }

        @Override
        public PeerResult start(Node node, Object message) {
            if (message == null || !(message instanceof DataPacket)) {
                return PeerResult.fail(node,
                        ActionStatus.create(DataPacket.BAD_DATA_PACKET, "message is not DataPacket"));
            }
            DataPacket packet = (DataPacket) message;
            ActionStatus status = packet.assureParam1(this.code, this.xid);
            if (status.isOK()) {
                return this.processDataPacket(node, packet);
            } else {
                return PeerResult.fail(node, status);
            }
        }

        /**
         * 处理收到的DataPacket，
         * 
         * @param node 送回此packet的节点
         * @param packet 收到的dataPacket, 此DataPacket已满足前述要求。
         * @return PeerResult not null, 通讯结果
         */
        abstract protected PeerResult processDataPacket(Node node, DataPacket packet);
    }

    private static final class YesOrNoAssembler extends DataPacketAssembler {
        private String refuseCode;

        public YesOrNoAssembler(String code, String refuseCode, String xid) {
            super(code, xid);
            this.refuseCode = refuseCode;
        }

        @Override
        protected PeerResult processDataPacket(Node node, DataPacket packet) {
            Maybe<Boolean> maybe = DataPacket.checkYesOrNo(packet.getParam2());
            if (!maybe.isRight()) {
                return PeerResult.fail(node, maybe.getLeft());
            }
            boolean accept = maybe.getRight();
            if (accept) {
                return PeerResult.approve(node, true);
            } else {
                return PeerResult.refuse(node, this.refuseCode, //
                        this.formatReason(node, (ActionStatus) packet.getParam3()));
            }
        }

        private String formatReason(Node node, Object param3) {
            if (ActionStatus.class.isInstance(param3)) {
                ActionStatus status = (ActionStatus) param3;
                return String.format("%s, %s, %s", node.toString(), status.getCode(), status.getDescription());
            }
            return String.valueOf(param3);
        }
    }

}
