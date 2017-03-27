package net.eric.tpc.proto;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.proto.PeerTransactionState.Stage;

public abstract class AbstractPeerTransManager<B> implements PeerTransactionManager<B> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPeerTransManager.class);

    abstract protected PeerDtLogger dtLogger();

    public ActionResult beginTrans(TransStartRec transNode, B b, PeerTransactionState<B> state) {
        assert (transNode != null);
        assert (state != null);
        assert (b != null);

        if (state.getStage() != Stage.NONE) {
            return new ActionResult(BizErrorCode.PEER_PRTC_ERROR, "Nested transaction not suppored");
        }

        state.setStage(Stage.BEGIN);
        state.setXid(transNode.getXid());

        System.out.println(b.toString());
        System.out.println(transNode.toString());
        System.out.println(state.toString());

        Future<ActionResult> vrf = startCalcuVoteResult(b);
        state.setVoteResult(vrf);

        return ActionResult.OK;
    }

    public ActionResult processVoteReq(String xid, PeerTransactionState<B> state) {
        if (!xid.equals(state.getXid())) {
            return new ActionResult(BizErrorCode.PEER_PRTC_ERROR,
                    "Got Difference xid, current is " + state.getXid() + " got " + xid);
        }
        if (state.getStage() != Stage.BEGIN) {
            return new ActionResult(BizErrorCode.PEER_PRTC_ERROR, "Got Vote message more than one time");
        }

        Future<ActionResult> f = state.getVoteResult();
        try {
            ActionResult r = f.get(2, TimeUnit.SECONDS);
            return r;
        } catch (Exception e) {
            logger.error("Waiting Vote result error", e);
            return ActionResult.create(BizErrorCode.INNER_EXCEPTION, "Unable to got vote result");
        }
    }

    public void processTransDecision(String xid, boolean commit, PeerTransactionState<B> state) {
        boolean errorHappen = false;
        if (!xid.equals(state.getXid())) {
            logger.debug("Decision Stage: Got Difference xid, current is " + state.getXid() + " got " + xid);
            errorHappen = true;
        }
        if (state.getStage() != Stage.VOTED) {
            logger.debug("Decision Stage: got DECISION before vote");
            errorHappen = true;
        }
        if (commit && !errorHappen) {
            this.performCommit(state);
        } else {
            this.performAbort(state);
        }
    }

    public void processTimeout(PeerTransactionState<B> state) {

    }

    protected Future<ActionResult> startCalcuVoteResult(B b) {
        return Futures.immediateFuture(ActionResult.OK);
    }

    protected void performCommit(PeerTransactionState state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do COMMIT actions");
        }
    }

    protected void performAbort(PeerTransactionState state) {
        if (logger.isDebugEnabled()) {
            logger.debug("do Abort actions");
        }
    }
}
