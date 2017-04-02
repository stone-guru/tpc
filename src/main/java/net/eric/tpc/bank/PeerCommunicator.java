package net.eric.tpc.bank;

import static net.eric.tpc.base.Pair.asPair;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.CommunicationRound.WaitType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.MinaCommunicator;
import net.eric.tpc.net.PeerResult;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.DecisionQuerier;
import net.eric.tpc.proto.RoundResult;

public class PeerCommunicator extends MinaCommunicator implements DecisionQuerier {
    private static final Logger logger = LoggerFactory.getLogger(PeerCommunicator.class);
    
    public PeerCommunicator(ExecutorService commuTaskPool, ExecutorService sequenceTaskPool) {
        super(commuTaskPool, sequenceTaskPool);
    }

    @Override
    public Optional<Decision> queryDecision(String xid, List<Node> peers) {
        ActionStatus connectStatus = this.connectPeers(peers, WaitType.WAIT_ONE);
        if (!connectStatus.isOK()) {
            return Optional.absent();
        }

        try {
            List<Pair<Node, Object>> requests = Lists.newArrayList();
            DataPacket dataPacket = new DataPacket(DataPacket.DECISION_QUERY, xid);
            for (Node node : peers) {
                requests.add(asPair(node, dataPacket));
            }
            Future<RoundResult> resultFuture = super.communicate(requests, RoundType.DOUBLE_SIDE, WaitType.WAIT_ONE,
                    new DecisionQueryAssembler(DataPacket.DECISION_ANSWER, xid));

            Maybe<RoundResult> result = Maybe.force(resultFuture);
            if(result.isRight()){
                return result.getRight().getAnOkResult(Decision.class); //maybe absent
            }else{
                logger.error("forece result for decision query " + result.getLeft());
                return Optional.absent(); 
            }
        } finally {
            this.closeConnections();
        }
    }

    private static class DecisionQueryAssembler extends PeerResult.OneItemAssembler {
        private String code;
        private String xid;

        public DecisionQueryAssembler(String code, String xid) {
            this.code = code;
            this.xid = xid;
        }

        @Override
        public PeerResult start(Node node, Object message) {
            DataPacket packet = (DataPacket) message;
            ActionStatus status = packet.assureParam1(this.code, this.xid);
            if (status.isOK()) {
                Maybe<Boolean> maybe = DataPacket.checkYesOrNo(packet.getParam2());
                if (!maybe.isRight()) {
                    return PeerResult.fail(node, maybe.getLeft());
                }
                boolean commited = maybe.getRight();
                return PeerResult.approve(node, commited ? Decision.COMMIT : Decision.ABORT);
            }
            return PeerResult.fail(node, status);
        }
    }
}
