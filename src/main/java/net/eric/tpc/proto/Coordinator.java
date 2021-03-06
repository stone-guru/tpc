package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.CoorBizStrategy.TaskPartition;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public class Coordinator<B> implements TransactionManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private DtLogger<B> dtLogger;

    private CoorBizStrategy<B> bizStrategy;

    private CommunicatorFactory<B> communicatorFactory;

    private KeyGenerator keyGenerator;

    private Node self;

    public ActionStatus transaction(B biz) {
        String xid = keyGenerator.nextKey("TABC");

        Maybe<TaskPartition<B>> taskEither = getBizStrategy().splitTask(xid, biz);
        if (!taskEither.isRight()) {
            logger.info("Invalid Biz Message", "Can not start trans " + taskEither.getLeft().toString());
            return taskEither.getLeft();
        }

        TaskPartition<B> task = taskEither.getRight();
        List<Node> peers = task.getParticipants();

        Either<ActionStatus, Communicator<B>> communicatorEither = getCommunicatorFactory().getCommunicator(peers);
        if (!communicatorEither.isRight()) {
            return communicatorEither.getLeft();
        }
        Communicator<B> communicator = communicatorEither.getRight();

        try {
            ActionStatus beginTransResult = this.askBeginTrans(xid, task, communicator);
            if (!beginTransResult.isOK()) {
                return beginTransResult;
            }

            ActionStatus voteResult = processVote(xid, task, communicator);
            if (!voteResult.isOK()) {
                return voteResult;
            }

            getDtLogger().recordDecision(xid, Decision.COMMIT);
            Future<RoundResult> notifyFuture = communicator.notifyDecision(xid, Decision.COMMIT, peers);

            @SuppressWarnings("unused")
            Future<Void> canNotCare = this.getBizStrategy().commit(xid, this.finishTransListener);

            try {
                notifyFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            getCommunicatorFactory().releaseCommunicator(communicator);
        }

        return ActionStatus.OK;
    }

    private BizActionListener finishTransListener = new BizActionListener() {
        @Override
        public void onSuccess(String xid) {
            Coordinator.this.dtLogger.markTransFinished(xid);
        }

        @Override
        public void onFailure(String xid) {
            logger.error("Transaction " + xid + " commit failed");
        }
    };

    private ActionStatus processVote(String xid, TaskPartition<B> task, Communicator<B> communicator) {
        final List<Node> peers = task.getParticipants();
        Future<ActionStatus> selfVoteFuture = getBizStrategy().prepareCommit(xid, task.getCoorTask());
        Future<RoundResult> peersVoteFuture = communicator.gatherVote(xid, peers);

        // getDtLogger().recordStart2PC(xid);

        ActionStatus voteResult = ActionStatus.force(selfVoteFuture);
        ActionStatus peersVoteResult = RoundResult.force(peersVoteFuture);

        if (voteResult.isOK() && peersVoteResult.isOK()) {
            return ActionStatus.OK;
        }

        if (!peersVoteFuture.isDone()) {
            throw new ShouldNotHappenException();
        }

        RoundResult voteCommuResult = null;
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

    private ActionStatus askBeginTrans(String xid, TaskPartition<B> task, Communicator<B> communicator) {
        TransStartRec transStart = new TransStartRec(xid, getSelf(), task.getParticipants());
        Future<RoundResult> beginTransFuture = communicator.askBeginTrans(transStart, task.getPeerTasks());
        getDtLogger().recordBeginTrans(transStart, task.getCoorTask(), true);

        ActionStatus result = RoundResult.force(beginTransFuture);
        if (result.isOK()) {
            return result;
        }

        if (beginTransFuture.isDone()) {
            List<Node> successPeers = null;
            try {
                successPeers = beginTransFuture.get().getSuccessNodes();
            } catch (Exception e) {
                throw new ShouldNotHappenException(e);
            }
            this.abandomTrans(xid, successPeers, communicator);
        }

        return result;
    }

    private void abandomTrans(String xid, List<Node> nodes, Communicator<B> communicator) {
        if (logger.isDebugEnabled()) {
            logger.debug(xid + " abandom ");
        }
        this.getDtLogger().recordDecision(xid, Decision.ABORT);
        communicator.notifyDecision(xid, Decision.ABORT, nodes);

        @SuppressWarnings("unused")
        Future<Void> canNotCare = this.bizStrategy.abort(xid, this.finishTransListener);

        this.getCommunicatorFactory().releaseCommunicator(communicator);
    }

    public DtLogger<B> getDtLogger() {
        return dtLogger;
    }

    public void setDtLogger(DtLogger<B> dtLogger) {
        this.dtLogger = dtLogger;
    }

    public CoorBizStrategy<B> getBizStrategy() {
        return bizStrategy;
    }

    public void setBizStrategy(CoorBizStrategy<B> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }

    public CommunicatorFactory<B> getCommunicatorFactory() {
        return communicatorFactory;
    }

    public void setCommunicatorFactory(CommunicatorFactory<B> communicatorFactory) {
        this.communicatorFactory = communicatorFactory;
    }

    public Node getSelf() {
        return self;
    }

    public void setSelf(Node self) {
        this.self = self;
    }

    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }
}
