package net.eric.tpc.proto;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.biz.BizCode;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.UnImplementedException;
import net.eric.tpc.proto.PeerTransactionState.Stage;

public class Panticipantor<B> implements PeerTransactionManager<B> {
    
    private static final Logger logger = LoggerFactory.getLogger(Panticipantor.class);

    private DtLogger<B> dtLogger;
    private PeerBizStrategy<B> bizStrategy;
    
    @Override
    public ActionStatus beginTrans(TransStartRec startRec, B bill, PeerTransactionState<B> state) {
        assert (startRec != null);
        assert (state != null);
        assert (bill != null);
        synchronized (state) {
            if (state.getStage() != Stage.NONE) {
                return new ActionStatus(BizCode.PEER_PRTC_ERROR, "Nested transaction not suppored");
            }
            
            state.setStage(Stage.BEGIN);
            state.setXid(startRec.getXid());

            this.dtLogger.recordBeginTrans(startRec, bill, false);
            
            System.out.println(bill.toString());
            System.out.println(startRec.toString());
            System.out.println(state.toString());

            Future<ActionStatus> future = bizStrategy.checkAndPrepare(startRec.getXid(), bill);
            state.setVoteFuture(future);

            return ActionStatus.OK;
        }
    }

    @Override
    public ActionStatus processVoteReq(String xid, PeerTransactionState<B> state) {
        if (!xid.equals(state.getXid())) {
            return new ActionStatus(BizCode.PEER_PRTC_ERROR,
                    "Got Difference xid, current is " + state.getXid() + " got " + xid);
        }
        if (state.getStage() != Stage.BEGIN) {
            return new ActionStatus(BizCode.PEER_PRTC_ERROR, "Got Vote message more than one time");
        }
        synchronized (state) {
            ActionStatus voteResult = ActionStatus.force(state.getVoteResult());
            Vote vote = (voteResult.isOK()) ? Vote.YES : Vote.NO;

            state.setStage(Stage.VOTED);
            state.setVote(vote);

            if (!Vote.YES.equals(vote)) {
                this.performAbort(state);
            }
            return voteResult;
        }
    }

    @Override
    public void processTransDecision(String xid, Decision decision, PeerTransactionState<B> state) {
        synchronized (state) {
            boolean messageWrong = false;
            if (!xid.equals(state.getXid())) {
                logger.debug("Decision Stage: Got Difference xid, current is " + state.getXid() + " got " + xid);
                messageWrong = true;
            }
            if (state.getStage() != Stage.VOTED) {
                logger.debug("Decision Stage: got DECISION before vote");
                messageWrong = true;
            }

            // 无意义消息，啥都不用做
            if (messageWrong) {
                return;
            }

            state.setStage(Stage.DECIDED);
            state.setDecision(decision);

            switch (decision) {
            case COMMIT:
                this.performCommit(state);
                break;
            case ABORT:
                this.performAbort(state);
                break;
            default:
                throw new UnImplementedException();
            }
        }
    }

    public void processTimeout(PeerTransactionState<B> state) {
        synchronized (state) {
            if (state.getStage().equals(Stage.BEGIN)) { // WANT VOTE_REQ
                this.performAbort(state);

            } else if (state.getStage().equals(Stage.VOTED)) {// WANT DECISION
                Optional<Decision> opt = this.queryDecisionFromOtherPeers(state);
                if (opt.isPresent()) {
                    switch (opt.get()) {
                    case ABORT:
                        this.performAbort(state);
                        break;
                    case COMMIT:
                        this.performCommit(state);
                        break;
                    default:
                        throw new UnImplementedException();
                    }
                } else {
                    this.hangUpTransaction(state);
                }
            }
        }
    }

    @Override
    public Optional<Decision> queryDecision(String xid){
        return dtLogger.getDecisionFor(xid);
    }

    private BizActionListener finishTransListener = new BizActionListener(){
        @Override
        public void onSuccess(String xid) {
            Panticipantor.this.dtLogger.markTransFinished(xid);
        }

        @Override
        public void onFailure(String xid) {
            logger.error("Transaction " + xid + " commit failed");
        }
    };
    
    private void performCommit(PeerTransactionState<B> state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do COMMIT actions");
        }
        this.bizStrategy.commit(state.getXid(), finishTransListener);
        state.setStage(Stage.ENDED);
    }

    private void performAbort(PeerTransactionState<B> state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do Abort actions");
        }
        this.bizStrategy.abort(state.getXid(), finishTransListener);
        state.setStage(Stage.ENDED);
    }

    private void hangUpTransaction(PeerTransactionState<B> state) {
        if (logger.isDebugEnabled()) {
            logger.debug("Transaction blocked " + state.getXid());
        }
    }

    private Optional<Decision> queryDecisionFromOtherPeers(PeerTransactionState<B> state) {
        int x = (int) Math.round(Math.random() * 10);
        switch (x % 3) {
        case 1:
            return Optional.of(Decision.COMMIT);
        case 2:
            return Optional.of(Decision.ABORT);
        default:
            return Optional.absent();
        }
    }

    public DtLogger<B> getDtLogger() {
        return dtLogger;
    }

    public void setDtLogger(DtLogger<B> dtLogger) {
        this.dtLogger = dtLogger;
    }

    public PeerBizStrategy<B> getBizStrategy() {
        return bizStrategy;
    }

    public void setBizStrategy(PeerBizStrategy<B> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }
}
