package net.eric.tpc.coor.stub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.proto.CommunicateResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransactionNodes;
import net.eric.tpc.proto.VoteResult;

public class MinaCommunicator implements Communicator<TransferMessage> {

	private Map<Node, MinaChannel> channelMap = new HashMap<Node, MinaChannel>();
	private ExecutorService executor = Executors.newFixedThreadPool(3);

	private MinaChannel getOrCreateChannel(Node node) {
		if (!channelMap.containsKey(node)) {
			MinaChannel channel = new MinaChannel();
			try {
				channel.connect(node);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			channelMap.put(node, channel);
		}
		return channelMap.get(node);
	}

	public FutureTask<CommunicateResult> askBeginTrans(TransactionNodes transNodes) {
		Node node = transNodes.getParticipants().get(0);
		String content = DataPacketCodec.encodeTransactionNodes(transNodes);
		DataPacket request = new DataPacket(DataPacket.BEGIN_TRANS, content);
		MinaChannel channel = getOrCreateChannel(node);
		final CommunicateResult cr = new CommunicateResult();
		try {
			DataPacket response = channel.request(request);
			cr.getSuccessNodes().add(node);
		} catch (Exception e) {
			e.printStackTrace();
			cr.getFailNodes().add(node);
		}
		FutureTask<CommunicateResult> task = new FutureTask<CommunicateResult>(new Callable<CommunicateResult>() {

			public CommunicateResult call() throws Exception {
				return cr;
			}
		});
		executor.submit(task);
		
		return task;
	}

	public Pair<List<Node>, List<Node>> dispatchTask(List<Pair<Node, TransferMessage>> tasks) {
		// TODO Auto-generated method stub
		return null;
	}

	public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
		// TODO Auto-generated method stub

	}

	public Future<VoteResult> gatherVote(String xid, List<Node> nodes) {
		// TODO Auto-generated method stub
		return null;
	}

	public void closeChannels(){
		for(MinaChannel channel : channelMap.values()){
			try {
				channel.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		executor.shutdown();
	}
}
