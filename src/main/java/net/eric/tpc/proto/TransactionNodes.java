package net.eric.tpc.proto;

import java.util.Collections;
import java.util.List;

public class TransactionNodes {
	private String xid;
	private Node coordinator;
	private List<Node> participants = Collections.emptyList();

	public TransactionNodes(){
	}
	
	public TransactionNodes(String xid, Node coordinator, List<Node> participants) {
		super();
		this.xid = xid;
		this.coordinator = coordinator;
		this.participants = participants;
	}

	public String getXid() {
		return xid;
	}

	public void setXid(String xid) {
		this.xid = xid;
	}

	public Node getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(Node coordinator) {
		this.coordinator = coordinator;
	}

	public List<Node> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Node> participants) {
		this.participants = participants;
	}

	@Override
	public String toString() {
		return "TransactionNodes [xid=" + xid + ", coordinator=" + coordinator + ", participants=" + participants + "]";
	}
}
