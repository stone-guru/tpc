package net.eric.tpc.coor.stub;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransactionNodes;
import net.eric.tpc.proto.VoteResult;
import net.eric.tpc.proto.CommuResult.PeerResult;

public class MinaCommunicator implements Communicator<TransferMessage> {
	
	public static final Function<Object, MinaChannel> OBJECT_AS_CHANNEL = new Function<Object, MinaChannel>() {
		public MinaChannel apply(Object obj) {
			return (MinaChannel) obj;
		}
	};

	public static final Function<Object, String> OBJECT_AS_STRING = new Function<Object, String>() {
		public String apply(Object obj) {
			return (String) obj;
		}
	};
		
	protected Map<Node, MinaChannel> channelMap;
	protected ExecutorService pool = Executors.newCachedThreadPool();
	protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);

	private MinaChannel getOrCreateChannel(Node node) {
		return null;
	}

	protected Optional<Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes) {
		Future<CommuResult> future = connectEach(nodes);
		CommuResult result;
		try {
			result = future.get();
			this.displayConnectResult(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return Optional.absent();
		} catch (ExecutionException e) {
			e.printStackTrace();
			return Optional.absent();
		}
		if (result.isAllOK()) {
			Map<Node, MinaChannel> map = new HashMap<Node, MinaChannel>();
			for (PeerResult r : result.getResults()) {
				map.put(r.peer(), (MinaChannel) r.result());
			}
			return Optional.of(map);
		}

		closeChannels(result.okResultAs(MinaCommunicator.OBJECT_AS_CHANNEL));
		return Optional.absent();
	}

	public FutureTask<CommuResult> askBeginTrans(TransactionNodes transNodes) {
///*		Node node = transNodes.getParticipants().get(0);
//		String content = DataPacketCodec.encodeTransactionNodes(transNodes);
//		DataPacket request = new DataPacket(DataPacket.BEGIN_TRANS, content);
//		MinaChannel channel = getOrCreateChannel(node);
//		final CommunicateResult cr = new CommunicateResult();
//		try {
//			DataPacket response = channel.request(request);
//			cr.getSuccessNodes().add(node);
//		} catch (Exception e) {
//			e.printStackTrace();
//			cr.getFailNodes().add(node);
//		}
//		FutureTask<CommunicateResult> task = new FutureTask<CommunicateResult>(new Callable<CommunicateResult>() {
//
//			public CommunicateResult call() throws Exception {
//				return cr;
//			}
//		});*/
		//executor.submit(task);

		return null;
	}

	public Pair<List<Node>, List<Node>> dispatchTask(List<Pair<Node, TransferMessage>> tasks) {
		return null;
	}

	public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
	}

	public Future<VoteResult> gatherVote(String xid, List<Node> nodes) {
		return null;
	}

	public void shutdown() {
		if(this.channelMap != null){
			this.closeChannels(this.channelMap.values());
		}
		this.pool.shutdown();
	}
	
	private void closeChannels(Iterable<MinaChannel> channels) {
		for (final MinaChannel channel : channels) {
			Runnable closeTask = new Runnable() {
				public void run() {
					try {
						channel.close();
					} catch (Exception e) {
						e.printStackTrace();
						// 没啥可做的动作了
					}
				}
			};
			pool.submit(closeTask);
		}
	}


	public Future<CommuResult> sendRequest(List<Pair<Node, Object>> requests) {
		CommunicationRound round = this.roundRef.get();
		if (round == null) {
			round = new CommunicationRound(this.pool);
			this.roundRef.set(round);
		}
		round.startNewRound(requests.size());

		for (final Pair<Node, Object> p : requests) {
			final MinaChannel channel = channelMap.get(p.fst());
			if (channel == null) {
				throw new InvalidParameterException("Given node not connected first " + p.fst());
			}
			Runnable task = new Runnable() {
				public void run() {
					System.out.println("begin send");
					channel.sendRequest(p.snd());
					System.out.println("after send");
				}
			};
			this.pool.submit(task);
		}
		return round.getResult();
	}
	
	private Future<CommuResult> connectEach(List<Node> nodes) {

		final CommunicationRound round = new CommunicationRound(pool);
		round.startNewRound(nodes.size());

		for (final Node node : nodes) {
			Runnable task = new Runnable() {
				public void run() {
					MinaChannel channel = new MinaChannel(node, MinaCommunicator.this.roundRef);
					try {
						channel.connect();
						System.out.println("begin reg");
						round.regSuccess(node, channel);
						System.out.println("finish reg");
					} catch (Exception e) {
						round.regFailure(node, "EXCEPTION", e.getMessage());
					}

				}
			};
			pool.submit(task);
		}
		return round.getResult();
	}
	
	public void displayConnectResult(CommuResult result) {
		if (result.isAllDone()) {
			System.out.println("All action is done");
		} else {
			System.out.println("Not all action is done");
		}

		for (PeerResult pr : result.getResults()) {
			if (pr.isRight()) {
				System.out.println("Action done for" + pr.peer());

			} else {
				System.out.println("Action fail for " + pr.peer() + ", by " + pr.errorCode() + ", " + pr.errorReason());
			}
		}
	}
}
