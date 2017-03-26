package net.eric.tpc.coor.main;

import java.util.Arrays;
import java.util.concurrent.Future;

import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;

public class Console {
	public static void main(String[] args) throws Exception {
		Communicator communicator = new MinaCommunicator();
		TransStartRec transNodes = new TransStartRec("TR09282", new Node("localhost", 987),
				Arrays.asList(new Node("127.0.0.1", 10024), new Node("198.2.2.2", 10088)));
		Future<CommuResult> task = null; //communicator.askBeginTrans(transNodes);
		
		CommuResult result = (CommuResult) task.get();
		
		System.out.println(result);

		communicator.shutdown();
	}
}
