package net.eric.tpc.net;

import net.eric.tpc.proto.PeerTransactionState;

public class HeartBeatRequestHandler implements RequestHandler {
    private static final DataPacket heartbeatAnswer = DataPacket.readOnlyPacket(DataPacket.HEART_BEAT_ANSWER, "", "");
    
    public DataPacket process(DataPacket request, PeerTransactionState state) {
        return heartbeatAnswer;
    }
    
    public String getCorrespondingCode() {
        return DataPacket.HEART_BEAT;
    }
}
