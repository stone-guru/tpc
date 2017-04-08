package net.eric.tpc.entity;

import java.util.Date;

import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.Vote;

public class DtRecord {
    private long xid;
    private Date startTime;
    private String coordinator;
    private String pariticipants;
    private boolean start2PC;
    private Vote vote;
    private Date voteTime;
    private Decision decision;
    private Date decisionTime;
    private Date finishTime;
    private byte[] bizMessage;

    public long getXid() {
        return xid;
    }

    public void setXid(long xid) {
        this.xid = xid;
    }

    public String getPariticipants() {
        return pariticipants;
    }

    public void setPariticipants(String pariticipants) {
        this.pariticipants = pariticipants;
    }

 
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public Date getVoteTime() {
        return voteTime;
    }

    public void setVoteTime(Date voteTime) {
        this.voteTime = voteTime;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public byte[] getBizMessage() {
        return bizMessage;
    }

    public void setBizMessage(byte[] bizMessage) {
        this.bizMessage = bizMessage;
    }

    public boolean isStart2PC() {
        return start2PC;
    }

    public void setStart2PC(boolean start2pc) {
        start2PC = start2pc;
    }

    public Date getDecisionTime() {
        return decisionTime;
    }

    public void setDecisionTime(Date decisionTime) {
        this.decisionTime = decisionTime;
    }
    
    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }
}
