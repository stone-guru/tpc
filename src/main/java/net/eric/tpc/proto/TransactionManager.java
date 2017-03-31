package net.eric.tpc.proto;

import net.eric.tpc.base.ActionStatus;

public interface TransactionManager<B> {
    ActionStatus  transaction(B biz);
}
