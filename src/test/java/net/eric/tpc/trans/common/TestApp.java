package net.eric.tpc.trans.common;

import static net.eric.tpc.common.Pair.asPair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.eric.tpc.biz.AccountIdentity;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.net.CommunicationRound.MessageAssembler;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.CommuResult.PeerResult;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;

public class TestApp extends MinaCommunicator {

	public static void main(String[] args) throws Exception {
		TestApp app = new TestApp();

		try {
			// app.connectOrAbort(nodes);
			// app.executeHttpCommand();
			app.executeStartTrans();
		} finally {
			app.shutdown();
		}
	}

	private void executeStartTrans() throws Exception {
		TransferMessage msg = new TransferMessage();
		msg.setTransSN("982872393");
		msg.setTransType(TransferMessage.INCOME);
		msg.setLaunchTime(new Date());
		msg.setAccount(new AccountIdentity("mike", "BOC"));
		msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
		msg.setReceivingBankCode("BOC");
		msg.setAmount(BigDecimal.valueOf(200));
		msg.setSummary("for cigrate");
		msg.setVoucherNumber("BIK09283-33843");

		Node boc = new Node("localhost", 10021);
		Node bbc = new Node("localhost", 10022);
		Node abc = new Node("localhost", 10023);
		Node bdc = new Node("localhost", 10024);

		TransStartRec st = new TransStartRec("KJDF000001", new Node("localhost", 9001),
				ImmutableList.of(boc, bbc, abc, bdc));

		super.connectPanticipants(ImmutableList.of(boc, bbc, abc, bdc));
		Future<CommuResult> result = null;
		try {
			 result = this.askBeginTrans(st,
					ImmutableList.of(asPair(boc, msg), asPair(bbc, msg), asPair(abc, msg), asPair(bdc, msg)));
		} finally {
			this.roundRef.get().clearLatch();
		}
		CommuResult r = result.get();

		displayConnectResult(r);
	}

	private void executeHttpCommand() {
		List<Node> nodes = ImmutableList.of(new Node("www.baidu.com", 80), // new
				// Node("locafdsflhost",
				// 80),
				new Node("www.zhihu.com", 80), new Node("www.sohu.com", 80), new Node("www.sina.com", 80),
				new Node("www.cnblogs.com", 80));
		this.connectPanticipants(nodes);

		List<Pair<Node, Object>> requests = this.generateHttpCommand(nodes);
		Future<CommuResult> resultFuture = sendRequest(requests, new HttpHeaderAssembler());
		try {
			CommuResult result = resultFuture.get();
			for (String s : result.okResultAs(TestApp.OBJECT_AS_STRING)) {
				System.out.println(s);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private List<Pair<Node, Object>> generateHttpCommand(List<Node> nodes) {
		List<Pair<Node, Object>> commands = Lists.newArrayList();
		for (Node node : nodes) {
			Object command = "HEAD /index.html HTTP/1.1\nHOST:" + node.getAddress() + "\n\n";
			commands.add(Pair.asPair(node, command));
		}
		return commands;
	}

	public static class HttpHeaderAssembler implements MessageAssembler {
		public PeerResult firstMessage(Node node, Object message) {
			return new PeerResult(node, message.toString(), false);
		}

		public PeerResult fold(Node node, PeerResult previous, Object message) {
			if (message.toString().length() == 0) {
				return previous.asDone();
			}
			return new PeerResult(node, previous.result().toString() + ", " + message.toString(), false);
		}

		public PeerResult transEnd(Node node, Optional<PeerResult> previous) {
			if (!previous.isPresent()) {
				return new PeerResult(node, "NO DATA", "NO data received");
			}
			return previous.get().asDone();
		}
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
