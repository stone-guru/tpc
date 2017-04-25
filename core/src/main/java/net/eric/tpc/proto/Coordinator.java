package net.eric.tpc.proto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.CoorBizStrategy.TaskPartition;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public class Coordinator<B> implements TransactionManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);
    public static final String XID_PREFIX = "TABC";

    @Inject
    private DtLogger dtLogger;

    @Inject
    @Named("CoorBizStrategy")
    private CoorBizStrategy<B> bizStrategy;

    @Inject
    private CommunicatorFactory communicatorFactory;

    @Inject
    private KeyGenerator keyGenerator;

    @Inject
    @Named("Coordinator InetSocketAddress")
    private InetSocketAddress self;

    private ExecutorService bizActionPool = Executors.newFixedThreadPool(2);

    @Override
    public ActionStatus transaction(B biz) {

        
        long xid = keyGenerator.nextKey(XID_PREFIX);

        Maybe<TaskPartition<B>> taskEither = bizStrategy.splitTask(xid, biz);
        if (!taskEither.isRight()) {
            logger.info("Can not start trans Invalid Biz Message " + taskEither.getLeft().toString());
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

            ActionStatus voteResult = this.processVote(xid, task, communicator);
            if (!voteResult.isOK()) {
                return voteResult;
            }

            this.dtLogger.recordDecision(xid, Decision.COMMIT);
            
            Future<RoundResult<Boolean>> notifyf= communicator.notifyDecision(xid, Decision.COMMIT, peers);

            this.execFinishAction(xid, this.bizStrategy::commit);
            
            try {
                notifyf.get();
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } finally {
            communicatorFactory.releaseCommunicator(communicator);
        }

        return ActionStatus.OK;
    }

    private void execFinishAction(long xid, Function<Long, Boolean> f) {
        Runnable commit = new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    success = f.apply(xid);
                } catch (Exception e) {
                    logger.error("exec biz commit for " + xid);
                }
                if (success)
                    Coordinator.this.dtLogger.markTransFinished(xid);
            }
        };
        bizActionPool.execute(commit);
    }

    private Future<ActionStatus> startLocalPrepare(long xid, B b) {
        return bizActionPool.submit(() -> bizStrategy.prepareCommit(xid, b));
    }

    private ActionStatus processVote(long xid, TaskPartition<B> task, Communicator communicator) {
        final List<InetSocketAddress> peers = task.getParticipants();
        Future<ActionStatus> selfVoteFuture = this.startLocalPrepare(xid, task.getCoorTask());
        Future<RoundResult<Boolean>> peersVoteFuture = communicator.gatherVote(xid, peers);

        ActionStatus voteResult = FutureTk.force(selfVoteFuture);
        ActionStatus peersVoteResult = FutureTk.waiting(peersVoteFuture, 2000);

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

    private ActionStatus askBeginTrans(long xid, TaskPartition<?> task, Communicator communicator) {
        TransStartRec transStart = new TransStartRec(xid, getSelf(), task.getParticipants());
        Future<RoundResult<Boolean>> beginTransFuture = communicator.askBeginTrans(transStart, task.getPeerTasks());
        this.dtLogger.recordBeginTrans(transStart, task.getCoorTask(), true);

        ActionStatus result = FutureTk.waiting(beginTransFuture, 2000);
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

        this.execFinishAction(xid, bizStrategy::abort);

        this.communicatorFactory.releaseCommunicator(communicator);
    }

    public void setDtLogger(DtLogger dtLogger) {
        this.dtLogger = dtLogger;
    }

    public CoorBizStrategy<B> getBizStrategy() {
        return bizStrategy;
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

    public void setBizStrategy(CoorBizStrategy<B> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }

    @Override
    public void close() throws IOException {
        if(this.bizActionPool != null)
            this.bizActionPool.shutdown();
    }
}
