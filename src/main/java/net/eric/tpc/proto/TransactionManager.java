package net.eric.tpc.proto;

public interface TransactionManager<B> {
    boolean beginTrans(TransStartRec transNode, B b, TransactionState state);
}
