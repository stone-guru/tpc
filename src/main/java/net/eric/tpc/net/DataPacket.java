package net.eric.tpc.net;

public class DataPacket {
	
	public static DataPacket readOnlyPacket(String code, String content) {
		return new DataPacket(code, content) {
			@Override
			public void setCode(String s) {
				throw new UnsupportedOperationException("code of readonlyPacket cannot be changed");
			}

			@Override
			public void setContent(String content) {
				throw new UnsupportedOperationException("content of readonlyPacket cannot be changed");
			}
		};
	}

	public static final String HEART_BEAT = "HEART_BEAT";
	public static final String BEGIN_TRANS = "BEGIN_TRANS";
	public static final String START_2PC = "START_2PC";
	public static final String VOTE_REQ = "VOTE_REQ";
	public static final String ANSWER_YES = "ANSWER_YES";
	public static final String ANSWER_NO = "ANSWER_NO";
	public static final String COMMIT = "COMMIT";
	public static final String ABORT = "ABORT";
	public static final String BIZ_MESSAGE = "BIZ_MESSAGE";
	public static final String CMD_OK = "CMD_OK";
	public static final String BAD_COMMNAD = "BAD_COMMAND";

	private String code;
	private String content;

	public DataPacket(String code, String content) {
		this.code = code;
		this.content = content;
	}

	public DataPacket() {
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "DataPacket [code=" + code + ", content=" + content + "]";
	}
}
