package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.common.ShouldNotHappenException;
import net.eric.tpc.proto.PeerTransactionState;

public class UnknowCommandRequestHandler<B> implements RequestHandler<B> {
    @Override
    public String getCorrespondingCode() {
        throw new ShouldNotHappenException();
    }

    @Override
    public Optional<DataPacket>  process(DataPacket request, PeerTransactionState<B> state) {
        return Optional.of(new DataPacket(DataPacket.BAD_COMMNAD, request.getCode()));
    }

    @Override
    public boolean requireTransState() {
        return false;
    }
}
