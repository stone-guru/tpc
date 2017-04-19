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

public class RequestHandlers {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandlers.class);

    public static <B> List<RequestHandler> getBasicHandlers(PeerTransactionManager<B> transManager) {
        return ImmutableList.of(new BeginTransRequestHandler<B>(transManager), //
                new VoteRequestHandler(transManager), //
                new TransDecisionHandler(transManager), //
                new PeerDecisonQueryHandler(transManager));
    }

    static class BeginTransRequestHandler<B> implements RequestHandler {
        
        private PeerTransactionManager<B> transManager;

        public BeginTransRequestHandler(PeerTransactionManager<B> transManager) {
            this.transManager = transManager;
        }

        @Override
        public short getCorrespondingCode() {
            return CommandCodes.BEGIN_TRANS;
        }

        private Maybe<Pair<TransStartRec, B>> peekBizEntities(Message request) {
            Maybe<TransStartRec> startRec = Maybe.safeCast(request.getParam(), TransStartRec.class, //
                    ErrorCode.BAD_DATA_PACKET, "param should be a TransStartRec");
            if (!startRec.isRight()) {
                return Maybe.fail(startRec.getLeft());
            }
            @SuppressWarnings("unchecked")
            Maybe<B> bill = (Maybe<B>) Maybe.fromNullable(request.getContent(), ErrorCode.BAD_DATA_PACKET,
                    "business entity is null");
            if (!bill.isRight()) {
                return bill.castLeft();
            }

            return Maybe.success(asPair(startRec.getRight(), bill.getRight()));
        }

        @Override
        public ProcessResult process(TransSession session, Message request) {
            ActionStatus r = null;

            Maybe<Pair<TransStartRec, B>> bizEntities = this.peekBizEntities(request);
            if (!bizEntities.isRight()) {
                r = bizEntities.getLeft();
            } else {
                r = this.transManager.beginTrans(bizEntities.getRight().fst(), bizEntities.getRight().snd());
            }

            Message response = r.isOK() ? //
                    Message.fromRequest(request, CommandCodes.BEGIN_TRANS_ANSWER, CommandCodes.YES) : //
                    Message.fromRequest(request, CommandCodes.BEGIN_TRANS_ANSWER, CommandCodes.NO, r);
            return new ProcessResult(response, !r.isOK());
        }
    }

    public static class VoteRequestHandler implements RequestHandler {

        private PeerTransactionManager<?> transManager;

        public VoteRequestHandler(PeerTransactionManager<?> transManager) {
            this.transManager = transManager;
        }

        @Override
        public short getCorrespondingCode() {
            return CommandCodes.VOTE_REQ;
        }

        @Override
        public ProcessResult process(TransSession transSession, Message request) {
            ActionStatus r = this.transManager.processVoteReq(request.getXid());
            Message response = r.isOK() ? //
                    Message.fromRequest(request, CommandCodes.VOTE_ANSWER, CommandCodes.YES) : //
                    Message.fromRequest(request, CommandCodes.VOTE_ANSWER, CommandCodes.NO, r);
            return new ProcessResult(response, !r.isOK());
        }
    }

    public static class TransDecisionHandler implements RequestHandler {

        private PeerTransactionManager<?>  transManager;

        public TransDecisionHandler(PeerTransactionManager<?>  transManager) {
            this.transManager = transManager;
        }

        @Override
        public short getCorrespondingCode() {
            return CommandCodes.TRANS_DECISION;
        }

        @Override
        public ProcessResult process(TransSession session, Message message) {
            switch (message.getCommandAnswer()) {
            case CommandCodes.YES:
                this.transManager.processTransDecision(message.getXid(), Decision.COMMIT);
                break;
            case CommandCodes.NO:
                this.transManager.processTransDecision(message.getXid(), Decision.ABORT);
                break;
            case CommandCodes.UNKNOWN:
                logger.info("unknown decison from this peer");
                break;
            default:
                logger.error("Unknown commit decision " + message.getCommandAnswer());
            }
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    }

    public static class PeerDecisonQueryHandler extends DecisionQueryHandler {

        private PeerTransactionManager<?>  transManager;

        public PeerDecisonQueryHandler(PeerTransactionManager<?>  transManager) {
            this.transManager = transManager;
        }

        @Override
        protected Optional<Decision> getDecisionFor(long xid) {
            return this.transManager.queryDecision(xid);
        }
    }

}
