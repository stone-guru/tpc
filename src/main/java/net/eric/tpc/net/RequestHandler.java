package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.PeerTransactionState;

public interface RequestHandler<B> {
    
    boolean requireTransState();

    String getCorrespondingCode();

    Pair<Optional<DataPacket>, Boolean> process(DataPacket request, PeerTransactionState<B> state);
}
