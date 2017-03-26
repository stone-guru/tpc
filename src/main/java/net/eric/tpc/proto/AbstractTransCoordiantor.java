package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.common.BankException;
import net.eric.tpc.common.ErrorMessage;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.Pair;

public abstract class AbstractTransCoordiantor<B> {

	protected abstract DtLogger dtLogger();

	protected abstract BizStrategy<B> bizStrategy();

	protected abstract Communicator<B> communicator();

	protected abstract Node mySelf();

	protected abstract Vote selfVote(String xid, B biz);

	public Optional<ErrorMessage>  transaction(B biz) throws BankException{
		Optional<ErrorMessage> err = bizStrategy().checkTransRequest(biz);
		if (err.isPresent()) {
			return err;
		}
		
		List<Pair<Node, B>> tasks = bizStrategy().splitTask(biz);
		String xid = KeyGenerator.nextKey("TABC");
		List<Node> nodes = getParticipants(tasks);

		if(!communicator().connectPanticipants(nodes)){
			throw new BankException(ErrorMessage.create(BizErrorCode.NODE_UNREACH, ""));
		}
		
		this.askBeginTrans(xid, tasks);
		VoteResult voteResult = processVote(xid, nodes);

///*		if (!voteResult.getErrorNodes().isEmpty()) {
//			abandomTrans(xid, voteResult.getYesNodes());
//			return false;
//		}
//
//		if (voteResult.getNoNodes().isEmpty() && selfVote(xid, biz) == Vote.YES) {
//			dtLogger().recordDecision(xid, Decision.COMMIT);
//			communicator().notifyDecision(xid, Decision.COMMIT, nodes);
//			return true;
//		} else {
//			dtLogger().recordDecision(xid, Decision.ABORT);
//			communicator().notifyDecision(xid, Decision.ABORT, voteResult.getYesNodes());
//			return false;
//		}*/
		return Optional.absent();
	}

	private boolean sendBusinessMessage(String xid, List<Pair<Node, B>> tasks) {

//		Pair<List<Node>, List<Node>> dispatchResult = communicator().dispatchTask(tasks);
//
//		if (!dispatchResult.snd().isEmpty()) {
//			abandomTrans(xid, dispatchResult.fst());
//			return false;
//		}

		return true;
	}

	private VoteResult processVote(String xid, List<Node> nodes) throws BankException {
		Future<VoteResult> fvr = communicator().gatherVote(xid, nodes);
		dtLogger().recordStart2PC(xid);

		VoteResult voteResult = null;
		try {
			voteResult = fvr.get();
		} catch (InterruptedException e) {
			throw new BankException(e);
		} catch (ExecutionException e) {
			throw new BankException(e);
		}

		return voteResult;
	}

	private void askBeginTrans(String xid, List<Pair<Node, B>> tasks) throws BankException{
		List<Node> nodes = getParticipants(tasks);
		TransStartRec transStart = new TransStartRec(xid, mySelf(), nodes);
		Future<CommuResult> fcr = communicator().askBeginTrans(transStart, tasks);

		dtLogger().recordBeginTrans(transStart);

		CommuResult beginTransResult = null;
		try {
			beginTransResult = fcr.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if (beginTransResult == null) {
			this.abandomTrans(xid, nodes);
		} else if (!beginTransResult.isAllOK()) {
			this.abandomTrans(xid, beginTransResult.getSuccessNodes());
		}
	}

	private List<Node> getParticipants(List<Pair<Node, B>> tasks) {
		List<Node> nodes = new ArrayList<Node>(tasks.size());
		for (Pair<Node, B> task : tasks) {
			nodes.add(task.fst());
		}
		return nodes;
	}

	private void abandomTrans(String xid, List<Node> nodes) {
		dtLogger().abandomTrans(xid);
		communicator().notifyDecision(xid, Decision.ABORT, nodes);
	}
}
