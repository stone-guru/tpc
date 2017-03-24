package net.eric.tpc.trans.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.eric.tpc.common.Pair;
import net.eric.tpc.coor.stub.MinaChannel;
import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.Node;

public class TestApp extends MinaCommunicator {

	private void executeHttpCommand(List<Node> nodes) {
		Optional<Map<Node, MinaChannel>> opt = this.connectOrAbort(nodes);
		if (!opt.isPresent()) {
			System.out.println("Not all node connected");
			return;
		}
		this.channelMap = opt.get();

		List<Pair<Node, Object>> requests = this.generateHttpCommand(nodes);
		Future<CommuResult> resultFuture = sendRequest(requests);
		try {
			CommuResult result = resultFuture.get();
			for(String s : result.okResultAs(TestApp.OBJECT_AS_STRING)){
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

	public static void main(String[] args) throws Exception {
		TestApp app = new TestApp();
		List<Node> nodes = ImmutableList.of(new Node("www.baidu.com", 80), //new Node("locafdsflhost", 80),
				new Node("www.zhihu.com", 80), new Node("www.sohu.com", 80), new Node("www.sina.com", 80), new Node("www.cnblogs.com", 80));
		try {
			//app.connectOrAbort(nodes);
			app.executeHttpCommand(nodes);
		} finally {
			app.shutdown();
		}
	}


}
