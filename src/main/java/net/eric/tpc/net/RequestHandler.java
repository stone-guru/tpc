package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.proto.PeerTransactionState;

public interface RequestHandler<B> {
    
    boolean requireTransState();

    String getCorrespondingCode();

    Optional<DataPacket> process(DataPacket request, PeerTransactionState<B> state);
}
