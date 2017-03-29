package net.eric.tpc.proto;

import net.eric.tpc.common.ActionStatus;

public interface TransactionManager<B> {
    ActionStatus  transaction(B biz);
}
