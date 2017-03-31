package net.eric.tpc.proto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import net.eric.tpc.base.Node;

public class TransStartRec implements Serializable {
    
    private static final long serialVersionUID = -2699980236712941307L;
    
    private String xid;
    private Node coordinator;
    private List<Node> participants = Collections.emptyList();

    public TransStartRec() {
    }

    public TransStartRec(String xid, Node coordinator, List<Node> participants) {
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
        return "TransStartRec [xid=" + xid + ", coordinator=" + coordinator + ", participants=" + participants + "]";
    }
}
