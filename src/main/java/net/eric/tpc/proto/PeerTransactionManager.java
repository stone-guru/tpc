package net.eric.tpc.proto;

import com.google.common.base.Optional;

import net.eric.tpc.common.ActionResult;

public interface PeerTransactionManager<B> {
    ActionResult beginTrans(TransStartRec transNode, B b, PeerTransactionState<B> state);

    ActionResult processVoteReq(String xid, PeerTransactionState<B> state);

    void processTransDecision(String xid, Decision decision, PeerTransactionState<B> state);

    void processTimeout(PeerTransactionState<B> state);
    
    Optional<Decision> queryDecision(String xid);
}
