package net.eric.tpc.net;

import net.eric.tpc.proto.TransactionState;

public interface RequestHandler {
    String getCorrespondingCode();
    DataPacket process(DataPacket request, TransactionState state);
}
