package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;

public class VoteResult {
    private List<Node> yesNodes = new ArrayList<Node>(4);
    private List<Node> noNodes = new ArrayList<Node>(4) ;
    private List<Node> errorNodes = new ArrayList<Node>(4);
    
    public List<Node> getYesNodes() {
        return yesNodes;
    }

    public List<Node> getNoNodes() {
        return noNodes;
    }

    public List<Node> getErrorNodes() {
        return errorNodes;
    }

}
