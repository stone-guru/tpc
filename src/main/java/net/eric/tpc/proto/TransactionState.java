package net.eric.tpc.proto;

public class TransactionState {
	public static enum Stage {
		NONE, BEGIN, BIZ_GOT, VOTED, DECIDED
	}

	private Stage stage = Stage.NONE;
	private String xid;
	private Vote vote;

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
}
