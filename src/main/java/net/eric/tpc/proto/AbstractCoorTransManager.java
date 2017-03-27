package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.BankException;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.Pair;

public abstract class AbstractCoorTransManager<B> implements CoorTransManager<B> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCoorTransManager.class);

    protected abstract CoorDtLogger dtLogger();

    protected abstract CoorBizStrategy<B> bizStrategy();

    protected abstract CoorCommunicator<B> communicator();

    protected abstract Node mySelf();

    protected abstract Vote selfVote(String xid, B biz);

    public ActionResult transaction(B biz) throws BankException {
        ActionResult r = bizStrategy().checkTransRequest(biz);
        if (!r.isOK()) {
            return r;
        }

        List<Pair<Node, B>> tasks = bizStrategy().splitTask(biz);
        String xid = KeyGenerator.nextKey("TABC");
        List<Node> nodes = getParticipants(tasks);

        if (!communicator().connectPanticipants(nodes)) {
            return ActionResult.create(BizErrorCode.NODE_UNREACH, "");
        }

        ActionResult beginTransResult = this.askBeginTrans(xid, tasks);
        if (!beginTransResult.isOK()) {
            return beginTransResult;
        }

        ActionResult voteResult = processVote(xid, nodes);
        if (!voteResult.isOK()) {
            return voteResult;
        }

        //FIX ME
        if (this.selfVote(xid, biz) == Vote.YES) {
            dtLogger().recordDecision(xid, Decision.COMMIT);
            this.communicator().notifyDecision(xid, Decision.COMMIT, nodes);
            return ActionResult.OK;
        } else {
            dtLogger().recordDecision(xid, Decision.ABORT);
            this.communicator().notifyDecision(xid, Decision.ABORT, nodes);
        }
        return ActionResult.OK;
    }

    private ActionResult processVote(String xid, List<Node> nodes) {
        Future<CoorCommuResult> fvr = communicator().gatherVote(xid, nodes);
        dtLogger().recordStart2PC(xid);

        CoorCommuResult voteResult = null;
        try {
            voteResult = fvr.get();
        } catch (InterruptedException e) {
            logger.error("process vote", e);
        } catch (ExecutionException e) {
            logger.error("process vote", e);
        }

        if (voteResult == null) {
            abandomTrans(xid, nodes);
            return ActionResult.INNER_ERROR;
        }

        if (voteResult.isAllOK()) {
            return ActionResult.OK;
        }

        this.abandomTrans(xid, voteResult.getSuccessNodes());
        return voteResult.getAnError();
    }

    private ActionResult askBeginTrans(String xid, List<Pair<Node, B>> tasks) throws BankException {
        List<Node> nodes = getParticipants(tasks);
        TransStartRec transStart = new TransStartRec(xid, mySelf(), nodes);
        Future<CoorCommuResult> fcr = communicator().askBeginTrans(transStart, tasks);

        dtLogger().recordBeginTrans(transStart);

        CoorCommuResult beginTransResult = null;
        try {
            beginTransResult = fcr.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (beginTransResult == null) {
            this.abandomTrans(xid, nodes);
        } else if (!beginTransResult.isAllOK()) {
            this.abandomTrans(xid, beginTransResult.getSuccessNodes());
        }

        return ActionResult.OK;
    }

    private List<Node> getParticipants(List<Pair<Node, B>> tasks) {
        List<Node> nodes = new ArrayList<Node>(tasks.size());
        for (Pair<Node, B> task : tasks) {
            nodes.add(task.fst());
        }
        return nodes;
    }

    private void abandomTrans(String xid, List<Node> nodes) {
        dtLogger().abandomTrans(xid);
        communicator().notifyDecision(xid, Decision.ABORT, nodes);
    }
}
