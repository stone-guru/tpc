package net.eric.tpc.proto;

import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;

public class PeerTransactionState<B> {
    public static enum Stage {
        NONE, BEGIN, VOTED, DECIDED, ENDED
    }

    private Stage stage = Stage.NONE;
    private String xid;
    private Vote vote;
    private Decision decision;
    private Future<ActionStatus> voteResult;
    private B bizEntity;
    private TransStartRec transStartRec;
    
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

    public Future<ActionStatus> getVoteFuture() {
        return voteResult;
    }

    public void setVoteFuture(Future<ActionStatus> voteResult) {
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

    public TransStartRec getTransStartRec() {
        return transStartRec;
    }

    public void setTransStartRec(TransStartRec transStartRec) {
        this.transStartRec = transStartRec;
    }
    
    
}
