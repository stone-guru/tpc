package net.eric.tpc.proto;

import com.google.common.base.Optional;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public interface PeerTransactionManager<B> {
    ActionStatus beginTrans(TransStartRec transNode, B b);

    ActionStatus processVoteReq(String xid);

    void processTransDecision(String xid, Decision decision);

    Optional<Decision> queryDecision(String xid);
}
