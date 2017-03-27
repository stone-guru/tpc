package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.BankException;
import net.eric.tpc.common.Either;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.ShouldNotHappenException;
import net.eric.tpc.proto.CoorBizStrategy.TaskPartition;

public abstract class AbstractCoorTransManager<B> implements CoorTransManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCoorTransManager.class);

    protected abstract DtLogger<B> dtLogger();

    protected abstract CoorBizStrategy<B> bizStrategy();

    protected abstract CommunicatorFactory<B> communicatorFactory();

    protected abstract Node mySelf();

    public ActionResult transaction(B biz) throws BankException {

        Either<ActionResult, TaskPartition<B>> taskEither = bizStrategy().splitTask(biz);
        if (!taskEither.isRight()) {
            logger.info("Invalid Biz Message", "Can not start trans " + taskEither.getLeft().toString());
            return taskEither.getLeft();
        }

        TaskPartition<B> task = taskEither.getRight();
        List<Node> peers = task.getParticipants();
        String xid = KeyGenerator.nextKey("TABC");

        Either<ActionResult, CoorCommunicator<B>> communicatorEither = communicatorFactory().getCommunicator(peers);
        if(!communicatorEither.isRight()){
            return communicatorEither.getLeft();
        }
        CoorCommunicator<B> communicator = communicatorEither.getRight();
        
        ActionResult beginTransResult = this.askBeginTrans(xid, task, communicator);
        if (!beginTransResult.isOK()) {
            return beginTransResult;
        }

        ActionResult voteResult = processVote(xid, task, communicator);
        if (!voteResult.isOK()) {
            return voteResult;
        }

        @SuppressWarnings("unused")
        Future<ActionResult> canNotCare = this.bizStrategy().commit(biz);

        dtLogger().recordDecision(xid, Decision.COMMIT);

        communicator.notifyDecision(xid, Decision.COMMIT, peers);

        communicatorFactory().releaseCommunicator(communicator);
        
        return ActionResult.OK;
    }

    private ActionResult processVote(String xid, TaskPartition<B> task, CoorCommunicator<B> communicator) {
        final List<Node> peers = task.getParticipants();
        Future<ActionResult> selfVoteFuture = bizStrategy().canCommit(task.getCoorTask());
        Future<CoorCommuResult> peersVoteFuture = communicator.gatherVote(xid, peers);

        dtLogger().recordStart2PC(xid);

        ActionResult voteResult = ActionResult.force(selfVoteFuture);
        ActionResult peersVoteResult = CoorCommuResult.force(peersVoteFuture);

        if (voteResult.isOK() && peersVoteResult.isOK()) {
            return ActionResult.OK;
        }

        if (!peersVoteFuture.isDone()) {
            throw new ShouldNotHappenException();
        }

        CoorCommuResult voteCommuResult = null;
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

    private ActionResult askBeginTrans(String xid, TaskPartition<B> task, CoorCommunicator<B>  communicator) {
        TransStartRec transStart = new TransStartRec(xid, mySelf(), task.getParticipants());
        Future<CoorCommuResult> beginTransFuture = communicator.askBeginTrans(transStart, task.getPeerTasks());
        dtLogger().recordBeginTrans(transStart, task.getCoorTask());

        ActionResult result = CoorCommuResult.force(beginTransFuture);
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

    private void abandomTrans(String xid, List<Node> nodes, CoorCommunicator<B> communicator) {
        this.dtLogger().recordDecision(xid, Decision.ABORT);
        communicator.notifyDecision(xid, Decision.ABORT, nodes);
        this.communicatorFactory().releaseCommunicator(communicator);
    }
}
