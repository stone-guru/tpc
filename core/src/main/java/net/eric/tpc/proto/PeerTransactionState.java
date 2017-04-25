package net.eric.tpc.proto;

import java.util.Date;
import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class PeerTransactionState {
    public static enum Stage {
        NONE, BEGIN, VOTED, DECIDED, ENDED
    }

    private Stage stage = Stage.NONE;
    private long xid;
    private Vote vote;
    private Decision decision;
    private Future<ActionStatus> voteResult;
    private Object bizEntity;
    private TransStartRec transStartRec;
    private Date startTime;
    private Date VoteTime;
    private Date decideTime;
    
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {

        this.stage = stage;
        if (stage == Stage.BEGIN) {
            this.startTime = new Date();
        } else if (stage == Stage.VOTED) {
            this.VoteTime = new Date();
        }else if(stage == Stage.DECIDED){
            this.decideTime = new Date();
        }
    }

    public boolean isTimedout(long currentMillis, long limit) {
        Date at = this.activeTime();
        if(at != null){
            //System.out.println(String.format("%d %d %d", currentMillis, at.getTime(), limit));
            return  currentMillis - at.getTime() >= limit;
        }
        return false;
    }

    public Date activeTime(){
        if (this.stage == Stage.BEGIN) {
            return this.startTime;
        }
        if (this.stage == Stage.VOTED){
            return this.VoteTime;
        }
        if (this.stage == Stage.DECIDED){
            return this.decideTime;
        }
        return null;
    }
    
    public long getXid() {
        return xid;
    }

    public void setXid(long xid) {
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

    public Object getBizEntity() {
        return bizEntity;
    }

    public void setBizEntity(Object bizEntity) {
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

    public Date getStartTime() {
        return startTime;
    }

    public Date getVoteTime() {
        return VoteTime;
    }

}
