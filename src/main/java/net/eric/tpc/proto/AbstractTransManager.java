package net.eric.tpc.proto;

import net.eric.tpc.proto.TransactionState.Stage;

public class AbstractTransManager<B> implements TransactionManager<B>{
	
	public boolean beginTrans(TransStartRec transNode, B b, TransactionState state){
		assert(transNode != null);
		assert(state != null);
		assert(b != null);
		
		if(state.getStage() != Stage.NONE){
			return false;
		}
		state.setStage(Stage.BEGIN);
		state.setXid(transNode.getXid());
		
		System.out.println(b.toString());
		System.out.println(transNode.toString());
		System.out.println(state.toString());
		
		return true;
	}
}
