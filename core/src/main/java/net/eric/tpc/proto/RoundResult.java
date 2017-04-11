package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.ActionStatus;

public interface RoundResult<T> {
    int wantedCount();
    boolean isAllDone();
    boolean isAllOK();
    List<T> okResults();
    int okResultCount();
    ActionStatus getAnError();
    List<InetSocketAddress> getSuccessNodes();
}
