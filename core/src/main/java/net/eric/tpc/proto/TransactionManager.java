package net.eric.tpc.proto;

import java.io.Closeable;

import net.eric.tpc.base.ActionStatus;

public interface TransactionManager<B> extends Closeable{
    ActionStatus  transaction(B biz);
}
