package net.eric.tpc.net;

import static net.eric.tpc.base.Pair.asPair;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;

public class PeerIoHandler extends AbstractIoHandler {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);
    private PeerTransactionManager<TransferBill> transManager;

    @Override
    protected List<RequestHandler> requestHandlers() {
        return ImmutableList.of(new BeginTransRequestHandler(), //
                new VoteRequestHandler(), //
                new TransDecisionHandler(), //
                new PeerDecisonQueryHandler());
    }

    public void setTransManager(PeerTransactionManager<TransferBill> transManager) {
        this.transManager = transManager;
    }

    class BeginTransRequestHandler implements RequestHandler {
        @Override
        public String getCorrespondingCode() {
            return DataPacket.BEGIN_TRANS;
        }

        private Maybe<Pair<TransStartRec, TransferBill>> peekBizEntities(DataPacket request) {
            Maybe<TransStartRec> startRec = Maybe.safeCast(request.getParam2(), TransStartRec.class, //
                    ErrorCode.BAD_DATA_PACKET, "param2 should be a TransStartRec");
            if (!startRec.isRight()) {
                return Maybe.fail(startRec.getLeft());
            }
            Maybe<TransferBill> bill = Maybe.safeCast(request.getParam3(), TransferBill.class, //
                    ErrorCode.BAD_DATA_PACKET, "param3 should be a TransStartRec");
            if (!bill.isRight()) {
                return Maybe.fail(bill.getLeft());
            }
            return Maybe.success(asPair(startRec.getRight(), bill.getRight()));
        }

        @Override
        public ProcessResult process(TransSession session, DataPacket request) {
            DataPacket response = null;
            ActionStatus r = null;

            Maybe<Pair<TransStartRec, TransferBill>> bizEntities = this.peekBizEntities(request);
            if (!bizEntities.isRight()) {
                r = bizEntities.getLeft();
            } else {
                TransStartRec startRec = bizEntities.getRight().fst();
                TransferBill bill = bizEntities.getRight().snd();
                r = PeerIoHandler.this.transManager.beginTrans(startRec, bill);
            }
            if (r.isOK()) {
                response = new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, request.getParam1(), DataPacket.YES);
            } else {
                response = new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, request.getParam1(), DataPacket.NO, r);
            }
            return new ProcessResult(response, !r.isOK());
        }
    }

    class VoteRequestHandler implements RequestHandler {
        @Override
        public String getCorrespondingCode() {
            return DataPacket.VOTE_REQ;
        }

        @Override
        public ProcessResult process(TransSession transSession, DataPacket request) {
            long xid = (Long) request.getParam1();

            ActionStatus r = PeerIoHandler.this.transManager.processVoteReq(xid);
            DataPacket response = null;
            if (r.isOK()) {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.YES);
            } else {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.NO, r);
            }
            return new ProcessResult(response, !r.isOK());
        }
    }

    class TransDecisionHandler implements RequestHandler {
        @Override
        public String getCorrespondingCode() {
            return DataPacket.TRANS_DECISION;
        }

        @Override
        public ProcessResult process(TransSession session, DataPacket request) {
            long xid = (Long) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.COMMIT);
            } else if (DataPacket.NO.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.ABORT);
            } else {
                logger.error(DataPacket.PEER_PRTC_ERROR, "Unknown commit decision " + decision);
            }
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    }

    class PeerDecisonQueryHandler extends DecisionQueryHandler {
        @Override
        protected Optional<Decision> getDecisionFor(long xid) {
            return PeerIoHandler.this.transManager.queryDecision(xid);
        }
    }

}
