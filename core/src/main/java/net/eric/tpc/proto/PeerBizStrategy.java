package net.eric.tpc.proto;

import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;

public interface PeerBizStrategy {
    <B> Future<ActionStatus> checkAndPrepare(long xid, B b);

    Future<Void> commit(long xid, BizActionListener listener);

    Future<Void> abort(long xid, BizActionListener listener);
}
