package net.eric.tpc.common;

import java.util.Date;

public class DtRecord {
    private String xid;
    private boolean initiative;
    private Date initTime;
    private String pariticipants;
    private String voteAnswer;
    private Date voteTime;
    private String actionCommand;
    private Date actionReceivedTime;
    private Date finishTime;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public boolean isInitiative() {
        return initiative;
    }

    public void setInitiative(boolean initiative) {
        this.initiative = initiative;
    }

    public Date getInitTime() {
        return initTime;
    }

    public void setInitTime(Date initTime) {
        this.initTime = initTime;
    }

    public String getPariticipants() {
        return pariticipants;
    }

    public void setPariticipants(String pariticipants) {
        this.pariticipants = pariticipants;
    }

    public String getVoteAnswer() {
        return voteAnswer;
    }

    public void setVoteAnswer(String voteAnswer) {
        this.voteAnswer = voteAnswer;
    }

    public Date getVoteTime() {
        return voteTime;
    }

    public void setVoteTime(Date voteTime) {
        this.voteTime = voteTime;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public Date getActionReceivedTime() {
        return actionReceivedTime;
    }

    public void setActionReceivedTime(Date actionReceivedTime) {
        this.actionReceivedTime = actionReceivedTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

}
