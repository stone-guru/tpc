package net.eric.tpc.proto;

import net.eric.tpc.base.ActionStatus;

public interface PeerBizStrategy<B> {
    
    ActionStatus checkAndPrepare(long xid, B b);

    boolean commit(long xid);

    boolean abort(long xid);
}
