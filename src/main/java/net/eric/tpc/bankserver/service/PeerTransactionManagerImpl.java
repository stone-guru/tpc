package net.eric.tpc.bankserver.service;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.AbstractPeerTransManager;
import net.eric.tpc.proto.DtLogger;

public class PeerTransactionManagerImpl extends AbstractPeerTransManager<TransferMessage> {
    @Override
    protected DtLogger<TransferMessage> dtLogger() {
        // TODO Auto-generated method stub
        return null;
    }
}
