package net.eric.tpc.proto;

import java.util.concurrent.Future;

import net.eric.tpc.common.ActionStatus;

public interface PeerBizStrategy<B> {
    Future<ActionStatus> checkAndPrepare(String xid, B b);

    Future<Void> commit(String xid, BizActionListener listener);

    Future<Void> abort(String xid, BizActionListener listener);
}
