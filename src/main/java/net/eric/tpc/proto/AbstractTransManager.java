package net.eric.tpc.proto;

import net.eric.tpc.proto.TransactionState.Stage;

public class AbstractTransManager<B> implements TransactionManager<B>{
	
	public boolean beginTrans(TransactionNodes transNode, TransactionState state){
		assert(transNode != null);
		assert(state != null);
		if(state.getStage() != Stage.NONE){
			return false;
		}
		state.setStage(Stage.BEGIN);
		state.setXid(transNode.getXid());
		return true;
	}
}
