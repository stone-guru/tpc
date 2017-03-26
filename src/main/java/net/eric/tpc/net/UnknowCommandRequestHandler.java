package net.eric.tpc.net;

import net.eric.tpc.proto.TransactionState;

public class UnknowCommandRequestHandler implements RequestHandler {

    public String getCorrespondingCode() {
        return null;
    }

    public DataPacket process(DataPacket request, TransactionState state) {
        return new DataPacket(DataPacket.BAD_COMMNAD, request.getCode());
    }

}
