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
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;

import static net.eric.tpc.net.binary.Message.fromRequest;

public class RequestHandlers {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandlers.class);

    public static List<RequestHandler> getBasicHandlers(PeerTransactionManager<?> transManager) {
        return ImmutableList.of(new BeginTransRequestHandler(transManager), //
                new VoteRequestHandler(transManager), //
                new TransDecisionHandler(transManager), //
                new PeerDecisionQueryHandler(transManager));
    }

    static class BeginTransRequestHandler implements RequestHandler {
        
        @SuppressWarnings("rawtypes")
        private PeerTransactionManager transManager;

        @SuppressWarnings("rawtypes")
        public BeginTransRequestHandler(PeerTransactionManager transManager) {
            this.transManager = transManager;
        }

        @Override
        public short[] getCorrespondingCodes() {
            return new short[]{CommandCodes.BEGIN_TRANS};
        }

        private Maybe<Pair<TransStartRec, Object>> peekBizEntities(Message request) {
            Maybe<TransStartRec> startRec = Maybe.safeCast(request.getParam(), TransStartRec.class, //
                    ErrorCode.BAD_DATA_PACKET, "param should be a TransStartRec");
            if (!startRec.isRight()) {
                return Maybe.fail(startRec.getLeft());
            }
            Maybe<Object> bill =  Maybe.fromNullable(request.getContent(), ErrorCode.BAD_DATA_PACKET,
                    "business entity is null");
            if (!bill.isRight()) {
                return bill.castLeft();
            }

            return Maybe.success(asPair(startRec.getRight(), bill.getRight()));
        }

        @SuppressWarnings("unchecked")
        @Override
        public ProcessResult process(TransSession session, Message request) {
            ActionStatus r = null;

            Maybe<Pair<TransStartRec, Object>> bizEntities = this.peekBizEntities(request);
            if (!bizEntities.isRight()) {
                r = bizEntities.getLeft();
            } else {
                r = this.transManager.beginTrans(bizEntities.getRight().fst(), bizEntities.getRight().snd());
            }

            Message response = r.isOK() ? fromRequest(request, CommandCodes.YES) : fromRequest(request,  CommandCodes.NO, r);
            return new ProcessResult(response, !r.isOK());
        }
    }


    public static class VoteRequestHandler implements RequestHandler {
        
        private PeerTransactionManager<?> transManager;

        public VoteRequestHandler(PeerTransactionManager<?> transManager) {
            this.transManager = transManager;
        }

        @Override
        public short[] getCorrespondingCodes() {
            return new short[]{CommandCodes.VOTE_REQ};
        }

        @Override
        public ProcessResult process(TransSession transSession, Message request) {
            ActionStatus r = this.transManager.processVoteReq(request.getXid());
            Message response = r.isOK() ? //
                    Message.fromRequest(request, CommandCodes.YES) : //
                    Message.fromRequest(request, CommandCodes.NO, r);
            return new ProcessResult(response, !r.isOK());
        }
    }

    public static class TransDecisionHandler implements RequestHandler {

        private PeerTransactionManager<?> transManager;

        public TransDecisionHandler(PeerTransactionManager<?> transManager) {
            this.transManager = transManager;
        }

        @Override
        public short[] getCorrespondingCodes() {
            return new short[]{CommandCodes.ABORT_TRANS, CommandCodes.COMMIT_TRANS};
        }

        @Override
        public ProcessResult process(TransSession session, Message message) {
            switch (message.getCode()) {
            case CommandCodes.COMMIT_TRANS:
                this.transManager.processTransDecision(message.getXid(), Decision.COMMIT);
                break;
            case CommandCodes.ABORT_TRANS:
                this.transManager.processTransDecision(message.getXid(), Decision.ABORT);
                break;
            default:
                logger.error("Unknown commit decision " + message.getCode());
            }
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    }

    public static class PeerDecisionQueryHandler extends DecisionQueryHandler {

        private PeerTransactionManager<?> transManager;

        public PeerDecisionQueryHandler(PeerTransactionManager<?> transManager) {
            this.transManager = transManager;
        }

        @Override
        protected Optional<Decision> getDecisionFor(long xid) {
            return this.transManager.queryDecision(xid);
        }
    }

}
