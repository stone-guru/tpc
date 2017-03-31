package net.eric.tpc.proto;

import com.google.common.base.Optional;

import net.eric.tpc.base.ActionStatus;

public interface PeerTransactionManager<B> {
    ActionStatus beginTrans(TransStartRec transNode, B b, PeerTransactionState<B> state);

    ActionStatus processVoteReq(String xid, PeerTransactionState<B> state);

    void processTransDecision(String xid, Decision decision, PeerTransactionState<B> state);

    void processTimeout(PeerTransactionState<B> state);
    
    Optional<Decision> queryDecision(String xid);
}
