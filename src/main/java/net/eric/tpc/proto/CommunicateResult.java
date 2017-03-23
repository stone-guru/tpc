package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;

public class CommunicateResult {
	private List<Node> successNodes = new ArrayList<Node>(4);
	private List<Node> failNodes = new ArrayList<Node>(4);

	public List<Node> getSuccessNodes() {
		return successNodes;
	}

	public List<Node> getFailNodes() {
		return failNodes;
	}
	
	public boolean noError() {
		return failNodes.isEmpty();
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("Success nodes:");
		for(Node n : successNodes){
			builder.append(n.toString()).append(", ");
		}
		builder.append("Failure nodes:");
		for(Node n : failNodes){
			builder.append(n.toString()).append(", ");
		}
		return builder.toString();
	}

}
