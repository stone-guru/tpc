package net.eric.tpc.coor.main;

import java.util.Arrays;
import java.util.concurrent.FutureTask;

import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.proto.CommunicateResult;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransactionNodes;

public class Console {
	public static void main(String[] args) throws Exception {
		MinaCommunicator communicator = new MinaCommunicator();
		TransactionNodes transNodes = new TransactionNodes("TR09282", new Node("localhost", 987),
				Arrays.asList(new Node("127.0.0.1", 10024), new Node("198.2.2.2", 10088)));
		FutureTask<CommunicateResult> task = communicator.askBeginTrans(transNodes);
		
		CommunicateResult result = task.get();
		
		System.out.println(result);

		communicator.closeChannels();
	}
}
