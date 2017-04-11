package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.proto.PeerTransactionState.Stage;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class Panticipantor implements PeerTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(Panticipantor.class);
    
    private static Comparator<PeerTransactionState> activeTimeComparator = new Comparator<PeerTransactionState>(){
        @Override
        public int compare(PeerTransactionState o1, PeerTransactionState o2) {
            return 0;
        }
    };
    
    private ConcurrentSkipListMap<Long, PeerTransactionState> transactionMap = new ConcurrentSkipListMap<Long, PeerTransactionState>();
    private Timer timer = new Timer();

    private DtLogger dtLogger;
    private PeerBizStrategy bizStrategy;
    private DecisionQuerier decisionQuerier;
    private ExecutorService taskPool = Executors.newFixedThreadPool(3);
    
    public Panticipantor() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Panticipantor.this.timerTask();
            }
        }, 1000, 3000);
        
        NightWatch.regCloseAction(new Runnable(){
            @Override
            public void run() {
                Panticipantor.this.close();
            }
        });
    }

    public void close(){
        this.timer.cancel();
        this.taskPool.shutdown();
    }
    
    @Override
    public <B> ActionStatus beginTrans(TransStartRec startRec, B bill) {
        assert (startRec != null);
        assert (bill != null);

        PeerTransactionState state = new PeerTransactionState();
        state.setStage(Stage.BEGIN);
        state.setXid(startRec.getXid());
        state.setTransStartRec(startRec);

        PeerTransactionState prev = transactionMap.putIfAbsent(startRec.xid(), state);
        if (prev != null) {
            return new ActionStatus(ErrorCode.XID_EXISTS, String.valueOf(startRec.xid()));
        }

        this.dtLogger.recordBeginTrans(startRec, bill, false);

        if (logger.isDebugEnabled()) {
            logger.debug("Begin Transaction : " + startRec.toString() + " " + bill.toString() + " " + state.toString());
        }

        Future<ActionStatus> future = bizStrategy.checkAndPrepare(startRec.getXid(), bill);
        state.setVoteFuture(future);

        return ActionStatus.OK;
    }

    @Override
    public ActionStatus processVoteReq(long xid) {
        PeerTransactionState state = this.transactionMap.get(xid);
        if (state == null) {
            return new ActionStatus(ErrorCode.PEER_PRTC_ERROR, "no trans action for this xid " + xid);
        }
        if (state.getStage() != Stage.BEGIN) {
            return new ActionStatus(ErrorCode.PEER_PRTC_ERROR, "Got Vote message more than one time");
        }

        synchronized (state) {
            ActionStatus voteResult = FutureTk.statusForce(state.getVoteFuture());
            Vote vote = (voteResult.isOK()) ? Vote.YES : Vote.NO;

            state.setStage(Stage.VOTED);
            state.setVote(vote);

            return voteResult;
        }
    }

    @Override
    public void processTransDecision(long xid, Decision decision) {
        PeerTransactionState state = this.transactionMap.get(xid);
        if (state == null) {
            logger.info("processTransDecison,  trans not exists now" + xid);
            return;
        }

        synchronized (state) {
            // 不支持在自己还没有投票的时候就收到COMMIT
            if (state.getStage() != Stage.VOTED && decision == Decision.COMMIT) {
                logger.debug("Decision Stage: got DECISION before vote " + xid);
                return;
            }

            state.setStage(Stage.DECIDED);
            state.setDecision(decision);

            this.transactionMap.remove(xid);

            if (decision == Decision.COMMIT) {
                this.performCommit(state);
            } else if (decision == Decision.ABORT) {
                this.performAbort(state);
            } else {
                throw new UnImplementedException();
            }
        }
    }

    private void timerTask() {
        final List<PeerTransactionState> outdatedTrans = Lists.newArrayList();
        final long now = new Date().getTime();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Panticipanter timer task");
        }
        for (long xid : this.transactionMap.keySet()) {
            PeerTransactionState state = this.transactionMap.get(xid);
            synchronized (state) {
                if (state.isTimedout(now, 3000)) {
                    logger.debug(xid + " timed out!");
                    this.transactionMap.remove(xid);
                    outdatedTrans.add(state);
                }
            }
        }

        if (!outdatedTrans.isEmpty()) {
            processOutdatedTrans(outdatedTrans);
        }
    }

    private Future<Void> processOutdatedTrans(List<PeerTransactionState> outdatedTrans) {
        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() {
                for (PeerTransactionState state : outdatedTrans) {
                    if (state.getStage() == Stage.BEGIN) { // WANT VOTE_REQ
                        Panticipantor.this.performAbort(state);// 直接放弃
                    } else if (state.getStage() == Stage.VOTED && state.getVote() == Vote.YES) {
                        Builder<InetSocketAddress> builder = ImmutableList.builder();
                        List<InetSocketAddress> peers = builder//
                                .add(state.getTransStartRec().getCoordinator())//
                                .addAll(state.getTransStartRec().getParticipants())//
                                .build();
                        // TODO 去除自己，但是需要将Node表达标准化为IP才可行，目前在响应侧忽略

                        Optional<Decision> opt = Panticipantor.this.decisionQuerier.queryDecision(state.getXid(),
                                peers);
                        if (opt.isPresent()) {
                            Panticipantor.this.processTransDecision(state.getXid(), opt.get());
                        } else {
                            Panticipantor.this.hangUpTransaction(state);
                        }
                    }
                }
                return null;
            }
        };
        return this.taskPool.submit(task);
    }

    @Override
    public Optional<Decision> queryDecision(long xid) {
        return dtLogger.getDecisionFor(xid);
    }

    private BizActionListener finishTransListener = new BizActionListener() {
        @Override
        public void onSuccess(long xid) {
            Panticipantor.this.dtLogger.markTransFinished(xid);
        }

        @Override
        public void onFailure(long xid) {
            logger.error("Transaction " + xid + " failed");
        }
    };

    private void performCommit(PeerTransactionState state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do COMMIT actions");
        }
        this.bizStrategy.commit(state.getXid(), finishTransListener);
        state.setStage(Stage.ENDED);
    }

    private void performAbort(PeerTransactionState state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do Abort actions");
        }
        this.bizStrategy.abort(state.getXid(), finishTransListener);
        state.setStage(Stage.ENDED);
    }

    private void hangUpTransaction(PeerTransactionState state) {
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

    public DtLogger getDtLogger() {
        return dtLogger;
    }

    public void setDtLogger(DtLogger dtLogger) {
        this.dtLogger = dtLogger;
    }

    public PeerBizStrategy getBizStrategy() {
        return bizStrategy;
    }

    public void setBizStrategy(PeerBizStrategy bizStrategy) {
        this.bizStrategy = bizStrategy;
    }
}
