package net.eric.tpc.net;

import net.eric.tpc.proto.PeerTransactionState;

public interface RequestHandler {
    String getCorrespondingCode();
    DataPacket process(DataPacket request, PeerTransactionState state);
}
