package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.CoorBizStrategy.TaskPartition;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class Coordinator<B> implements TransactionManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private DtLogger dtLogger;

    private CoorBizStrategy<B> bizStrategy;

    private CommunicatorFactory communicatorFactory;

    private KeyGenerator keyGenerator;

    private InetSocketAddress self;

    public ActionStatus transaction(B biz) {
        long xid = keyGenerator.nextKey("TABC");

        Maybe<TaskPartition<B>> taskEither = getBizStrategy().splitTask(xid, biz);
        if (!taskEither.isRight()) {
            logger.info("Invalid Biz Message", "Can not start trans " + taskEither.getLeft().toString());
            return taskEither.getLeft();
        }

        TaskPartition<B> task = taskEither.getRight();
        List<InetSocketAddress> peers = task.getParticipants();

        Maybe<Communicator> communicatorEither = this.communicatorFactory.getCommunicator(peers);
        if (!communicatorEither.isRight()) {
            return communicatorEither.getLeft();
        }
        Communicator communicator = communicatorEither.getRight();

        try {
            ActionStatus beginTransResult = this.askBeginTrans(xid, task, communicator);
            if (!beginTransResult.isOK()) {
                return beginTransResult;
            }

            ActionStatus voteResult = processVote(xid, task, communicator);
            if (!voteResult.isOK()) {
                return voteResult;
            }

            this.dtLogger.recordDecision(xid, Decision.COMMIT);
            Future<RoundResult<Boolean>> notifyFuture = communicator.notifyDecision(xid, Decision.COMMIT, peers);

            @SuppressWarnings("unused")
            Future<Void> canNotCare = this.getBizStrategy().commit(xid, this.finishTransListener);

            try {
                notifyFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            communicatorFactory.releaseCommunicator(communicator);
        }

        return ActionStatus.OK;
    }

    private BizActionListener finishTransListener = new BizActionListener() {
        @Override
        public void onSuccess(long xid) {
            Coordinator.this.dtLogger.markTransFinished(xid);
        }

        @Override
        public void onFailure(long xid) {
            logger.error("Transaction " + xid + " commit failed");
        }
    };

    private ActionStatus processVote(long xid, TaskPartition<B> task, Communicator communicator) {
        final List<InetSocketAddress> peers = task.getParticipants();
        Future<ActionStatus> selfVoteFuture = getBizStrategy().prepareCommit(xid, task.getCoorTask());
        Future<RoundResult<Boolean>> peersVoteFuture = communicator.gatherVote(xid, peers);


        ActionStatus voteResult = FutureTk.statusForce(selfVoteFuture);
        ActionStatus peersVoteResult = FutureTk.resultForce(peersVoteFuture);

        if (voteResult.isOK() && peersVoteResult.isOK()) {
            return ActionStatus.OK;
        }

        if (!peersVoteFuture.isDone()) {
            throw new ShouldNotHappenException();
        }

        RoundResult<Boolean> voteCommuResult = null;
        try {
            voteCommuResult = peersVoteFuture.get();
        } catch (Exception e) {
            throw new ShouldNotHappenException(e);
        }
        this.abandomTrans(xid, voteCommuResult.getSuccessNodes(), communicator);

        if (!voteResult.isOK()) {
            return voteResult;
        } else {
            return peersVoteResult;
        }
    }

    private ActionStatus askBeginTrans(long xid, TaskPartition<B> task, Communicator communicator) {
        TransStartRec transStart = new TransStartRec(xid, getSelf(), task.getParticipants());
        Future<RoundResult<Boolean>> beginTransFuture = communicator.askBeginTrans(transStart, task.getPeerTasks());
        this.dtLogger.recordBeginTrans(transStart, task.getCoorTask(), true);

        ActionStatus result = FutureTk.resultForce(beginTransFuture);
        if (result.isOK()) {
            return result;
        }

        if (beginTransFuture.isDone()) {
            List<InetSocketAddress> successPeers = null;
            try {
                successPeers = beginTransFuture.get().getSuccessNodes();
            } catch (Exception e) {
                throw new ShouldNotHappenException(e);
            }
            this.abandomTrans(xid, successPeers, communicator);
        }

        return result;
    }

    private void abandomTrans(long xid, List<InetSocketAddress> nodes, Communicator communicator) {
        if (logger.isDebugEnabled()) {
            logger.debug(xid + " abandom ");
        }
        this.dtLogger.recordDecision(xid, Decision.ABORT);
        communicator.notifyDecision(xid, Decision.ABORT, nodes);

        @SuppressWarnings("unused")
        Future<Void> canNotCare = this.bizStrategy.abort(xid, this.finishTransListener);

        this.communicatorFactory.releaseCommunicator(communicator);
    }

    public void setDtLogger(DtLogger dtLogger) {
        this.dtLogger = dtLogger;
    }

    public CoorBizStrategy<B> getBizStrategy() {
        return bizStrategy;
    }

    public void setBizStrategy(CoorBizStrategy<B> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }

    public void setCommunicatorFactory(CommunicatorFactory communicatorFactory) {
        this.communicatorFactory = communicatorFactory;
    }

    public InetSocketAddress getSelf() {
        return self;
    }

    public void setSelf(InetSocketAddress self) {
        this.self = self;
    }

    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }
}
