package net.eric.tpc.proto;

import net.eric.tpc.common.ActionResult;

public interface PeerTransactionManager<B> {
    ActionResult beginTrans(TransStartRec transNode, B b, PeerTransactionState<B> state);

    ActionResult processVoteReq(String xid, PeerTransactionState<B> state);

    void processTransDecision(String xid, boolean commit, PeerTransactionState<B> state);

    void processTimeout(PeerTransactionState<B> state);
}
