package net.eric.tpc.bankserver.service;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.AbstractPeerTransManager;
import net.eric.tpc.proto.PeerDtLogger;

public class PeerTransactionManagerImpl extends AbstractPeerTransManager<TransferMessage> {
    @Override
    protected PeerDtLogger dtLogger() {
        // TODO Auto-generated method stub
        return null;
    }
}
