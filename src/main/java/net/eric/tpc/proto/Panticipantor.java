package net.eric.tpc.proto;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.proto.PeerTransactionState.Stage;

public class Panticipantor<B> implements PeerTransactionManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(Panticipantor.class);

    private DtLogger<B> dtLogger;
    private PeerBizStrategy<B> bizStrategy;
    private DecisionQuerier decisionQuerier;

    @Override
    public ActionStatus beginTrans(TransStartRec startRec, B bill, PeerTransactionState<B> state) {
        assert (startRec != null);
        assert (state != null);
        assert (bill != null);
        synchronized (state) {
            if (state.getStage() != Stage.NONE) {
                return new ActionStatus(DataPacket.PEER_PRTC_ERROR, "Nested transaction not suppored");
            }

            state.setStage(Stage.BEGIN);
            state.setXid(startRec.getXid());
            state.setTransStartRec(startRec);

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
            return new ActionStatus(DataPacket.PEER_PRTC_ERROR,
                    "Got Difference xid, current is " + state.getXid() + " got " + xid);
        }
        if (state.getStage() != Stage.BEGIN) {
            return new ActionStatus(DataPacket.PEER_PRTC_ERROR, "Got Vote message more than one time");
        }
        synchronized (state) {
            ActionStatus voteResult = ActionStatus.force(state.getVoteFuture());
            Vote vote = (voteResult.isOK()) ? Vote.YES : Vote.NO;

            state.setStage(Stage.VOTED);
            state.setVote(vote);

            return voteResult;
        }
    }

    @Override
    public void processTransDecision(String xid, Decision decision, PeerTransactionState<B> state) {
        synchronized (state) {
            if (!xid.equals(state.getXid())) {
                logger.debug("Decision Stage: Got Difference xid, current is " + state.getXid() + " got " + xid);
                return;
            }

            // 不允许在自己还没有投票的时候就收到COMMIT
            if (state.getStage() != Stage.VOTED && decision == Decision.COMMIT) {
                logger.debug("Decision Stage: got DECISION before vote " + xid);
                return;
            }

            state.setStage(Stage.DECIDED);
            state.setDecision(decision);

            if (decision == Decision.COMMIT) {
                this.performCommit(state);
            } else if (decision == Decision.ABORT) {
                this.performAbort(state);
            } else {
                throw new UnImplementedException();
            }
        }
    }

    public void processTimeout(PeerTransactionState<B> state) {
        synchronized (state) {
            if (state.getStage().equals(Stage.BEGIN)) { // WANT VOTE_REQ
                this.performAbort(state);// 直接放弃
            } else if (state.getStage() == Stage.VOTED && state.getVote() == Vote.YES) { // 投了YES却没有收到Decision
                
                List<Node> peers = Lists.newArrayList();
                Collections.copy(peers, state.getTransStartRec().getParticipants());
                peers.add(state.getTransStartRec().getCoordinator());
                // TODO 去除自己，但是需要将Node表达标准化为IP才可行，目前在响应侧忽略

                Optional<Decision> opt = this.decisionQuerier.queryDecision(state.getXid(), peers);
                if (opt.isPresent()) {
                    this.processTransDecision(state.getXid(), opt.get(), state);
                } else {
                    this.hangUpTransaction(state);
                }
            }
            // 其它状态无需处理
        }
    }

    @Override
    public Optional<Decision> queryDecision(String xid) {
        return dtLogger.getDecisionFor(xid);
    }

    private BizActionListener finishTransListener = new BizActionListener() {
        @Override
        public void onSuccess(String xid) {
            Panticipantor.this.dtLogger.markTransFinished(xid);
        }

        @Override
        public void onFailure(String xid) {
            logger.error("Transaction " + xid + " failed");
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

    public DecisionQuerier getDecisionQuerier() {
        return decisionQuerier;
    }

    public void setDecisionQuerier(DecisionQuerier decisionQuerier) {
        this.decisionQuerier = decisionQuerier;
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
