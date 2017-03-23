package net.eric.tpc.proto;

public interface TransactionManager<B> {
	boolean beginTrans(TransactionNodes transNode, TransactionState state);
}
