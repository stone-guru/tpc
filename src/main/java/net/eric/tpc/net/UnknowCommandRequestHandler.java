package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.Pair;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.PeerTransactionState;

public class UnknowCommandRequestHandler<B> implements RequestHandler<B> {
    @Override
    public String getCorrespondingCode() {
        throw new ShouldNotHappenException();
    }

    @Override
    public Pair<Optional<DataPacket>, Boolean>  process(DataPacket request, PeerTransactionState<B> state) {
        return Pair.asPair(Optional.absent(), true);
    }

    @Override
    public boolean requireTransState() {
        return false;
    }
}
