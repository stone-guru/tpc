package net.eric.tpc.coor.stub;

import java.security.InvalidParameterException;
import java.util.Collections;
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
import com.google.common.collect.Lists;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import static net.eric.tpc.common.Pair.asPair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.MessageAssembler;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.CommuResult.PeerResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.VoteResult;

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

	protected Map<Node, MinaChannel> channelMap = Collections.emptyMap();
	protected ExecutorService pool = Executors.newCachedThreadPool();
	protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);

	@Override
	public boolean connectPanticipants(List<Node> nodes) {
		Optional<Map<Node, MinaChannel>> opt = this.connectOrAbort(nodes);
		if (!opt.isPresent()) {
			System.out.println("Not all node connected");
			return false;
		}
		this.channelMap = opt.get();
		return true;
	}

	@Override
	public Future<CommuResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, TransferMessage>> tasks) {
		List<Pair<Node, Object>> requests = Lists.newArrayList();
		for (Pair<Node, TransferMessage> p : tasks) {
			final String param1 = DataPacketCodec.encodeTransStartRec(transStartRec);
			final String param2 = DataPacketCodec.encodeTransferMessage(p.snd());
			DataPacket packet = new DataPacket(DataPacket.BEGIN_TRANS, param1, param2);
			final Object msg = DataPacketCodec.encodeDataPacket(packet);
			requests.add(asPair(p.fst(), msg));
		}
		return this.sendRequest(requests, MinaCommunicator.BEGIN_TRANS_ASSEMBLER);
	}

	private static final MessageAssembler BEGIN_TRANS_ASSEMBLER = new CommunicationRound.OneItemAssembler() {
		@Override
		public PeerResult firstMessage(Node node, Object message) {
			DataPacket packet = DataPacketCodec.decodeDataPacket(message.toString());
			if (DataPacket.BEGIN_TRANS_ANSWER.equals(packet.getCode())) {
				if (DataPacket.YES.equals(packet.getParam1())) {
					return new PeerResult(node, true);
				} else {
					return new PeerResult(node, "CAN_NOT_BEGIN_TRANS", packet.getParam2());
				}
			} else {
				return new PeerResult(node, "CAN_NOT_BEGIN_TRANS", packet.toString());
			}
		}
	};

	public Future<VoteResult> gatherVote(String xid, List<Node> nodes){
		return null;
	}
	
	public Future<CommuResult> gatherVote1(String xid, List<Node> nodes) {
		List<Pair<Node, Object>> requests = Lists.newArrayList();
		for (Node n : nodes) {
			DataPacket packet = new DataPacket(DataPacket.VOTE_REQ, xid);
			final Object msg = DataPacketCodec.encodeDataPacket(packet);
			requests.add(asPair(n, msg));
		}
		 return this.sendRequest(requests, MinaCommunicator.VOTE_REQ_ASSEMBLER);
	}

	private static final MessageAssembler VOTE_REQ_ASSEMBLER = new CommunicationRound.OneItemAssembler() {
		@Override
		public PeerResult firstMessage(Node node, Object message) {
			DataPacket packet = DataPacketCodec.decodeDataPacket(message.toString());
			if (DataPacket.VOTE_ANSWER.equals(packet.getCode())) {
				if (DataPacket.YES.equals(packet.getParam1())) {
					return new PeerResult(node, true);
				} else if (DataPacket.NO.equals(packet.getParam1())) {
					return new PeerResult(node, DataPacket.NO, packet.getParam2());
				}
			}
			return new PeerResult(node, "VOTE ANSWER WRONG", packet.toString());
		}
	};

	public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
	}

	public void shutdown() {
		this.closeChannels(this.channelMap.values());
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
		return this.sendRequest(requests, CommunicationRound.ONE_ITEM_ASSEMBLER);
	}

	public Future<CommuResult> sendRequest(List<Pair<Node, Object>> requests, MessageAssembler assembler) {
		return this.communicate(requests, RoundType.DOUBLE_SIDE, assembler);
	}

	private Future<CommuResult> sendMessage(List<Pair<Node, Object>> messages) {
		return this.communicate(messages, RoundType.SINGLE_SIDE, CommunicationRound.ONE_ITEM_ASSEMBLER);
	}

	private Future<CommuResult> communicate(List<Pair<Node, Object>> messages, final RoundType roundType,
			MessageAssembler assembler) {
		for (final Pair<Node, Object> p : messages) {
			if (!this.channelMap.containsKey(p.fst())) {
				throw new IllegalStateException("Given node not connected first " + p.fst());
			}
		}
		final CommunicationRound round = this.newRound(messages.size(), roundType, assembler);

		for (final Pair<Node, Object> p : messages) {
			final MinaChannel channel = this.channelMap.get(p.fst());
			Runnable task = new Runnable() {
				public void run() {
					System.out.println("before call channel send");
					final boolean sendOnly = roundType == RoundType.SINGLE_SIDE;
					channel.sendMessage(p.snd(), sendOnly);
					System.out.println("after call channel send");
				}
			};
			this.pool.submit(task);
		}
		return round.getResult();
	}

	private Future<CommuResult> connectEach(List<Node> nodes) {
		final CommunicationRound round = this.newRound(nodes.size(), RoundType.SINGLE_SIDE,
				CommunicationRound.ONE_ITEM_ASSEMBLER);

		for (final Node node : nodes) {
			Runnable task = new Runnable() {
				public void run() {
					MinaChannel channel = new MinaChannel(node, MinaCommunicator.this.roundRef);
					try {
						channel.connect();
						System.out.println("begin reg");
						round.regMessage(node, channel);
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

	private CommunicationRound newRound(int peerCount, RoundType roundType, MessageAssembler assembler) {
		CommunicationRound round = new CommunicationRound(this.pool, peerCount, roundType, assembler);
		this.roundRef.set(round);
		return round;
	}

	public void closeConnections() {
		// TODO Auto-generated method stub

	}

	private Optional<Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes) {
		Future<CommuResult> future = connectEach(nodes);
		CommuResult result;
		try {
			result = future.get();
			// this.displayConnectResult(result);
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
}
