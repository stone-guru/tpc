package net.eric.tpc.proto;

import java.util.concurrent.Future;

import net.eric.tpc.common.ActionResult;

public class PeerTransactionState<B> {
    public static enum Stage {
        NONE, BEGIN, VOTED, DECIDED, ENDED
    }

    private Stage stage = Stage.NONE;
    private String xid;
    private Vote vote;
    private Decision decision;
    private Future<ActionResult> voteResult;
    private B bizEntity;
    
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public Future<ActionResult> getVoteResult() {
        return voteResult;
    }

    public void setVoteResult(Future<ActionResult> voteResult) {
        this.voteResult = voteResult;
    }

    public B getBizEntity() {
        return bizEntity;
    }

    public void setBizEntity(B bizEntity) {
        this.bizEntity = bizEntity;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
