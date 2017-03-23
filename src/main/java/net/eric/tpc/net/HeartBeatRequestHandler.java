package net.eric.tpc.net;

import net.eric.tpc.proto.TransactionState;

public class HeartBeatRequestHandler implements RequestHandler {
    private static final DataPacket cmdOk = DataPacket.readOnlyPacket(DataPacket.CMD_OK, "");
    
	public DataPacket process(DataPacket request, TransactionState state) {
		return cmdOk;
	}
	public String getCorrespondingCode() {
		return DataPacket.HEART_BEAT;
	}
}
