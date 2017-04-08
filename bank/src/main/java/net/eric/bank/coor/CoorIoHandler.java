package net.eric.bank.coor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import net.eric.bank.entity.TransferBill;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.AbstractIoHandler;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DecisionQueryHandler;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.TransSession;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;

public class CoorIoHandler extends AbstractIoHandler {
    
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(CoorIoHandler.class);

    private TransactionManager<TransferBill> transactionManager;
    private DtLogger<TransferBill> dtLogger;

    @Override
    protected List<RequestHandler> requestHandlers() {
        return ImmutableList.of(new TransferBillHandler(), new PeerDecisonQueryHandler());
    }

    private class TransferBillHandler implements RequestHandler {

        @Override
        public String getCorrespondingCode() {
            return DataPacket.TRANS_BILL;
        }

        @Override
        public ProcessResult process(TransSession session, DataPacket request) {
            Maybe<TransferBill> billMaybe = Maybe.safeCast(request.getParam1(), TransferBill.class, //
                    ErrorCode.PEER_PRTC_ERROR, "param1 is not a TransferBill");
            DataPacket reponse = null;
            if (billMaybe.isRight()) {
                ActionStatus actionStatus = CoorIoHandler.this.transactionManager.transaction(billMaybe.getRight());
                reponse = DataPacket.fromActionStatus(DataPacket.TRANS_BILL_ANSWER, actionStatus);
            } else {
                reponse = new DataPacket(DataPacket.TRANS_BILL_ANSWER, DataPacket.NO,
                        ActionStatus.create(ErrorCode.BAD_DATA_PACKET, "param1 should be a TransferBill"));
            }

            return new ProcessResult(reponse, false);
        }
    }

    class PeerDecisonQueryHandler extends DecisionQueryHandler {
        @Override
        protected Optional<Decision> getDecisionFor(long xid) {
            return CoorIoHandler.this.dtLogger.getDecisionFor(xid);
        }
    }

    public TransactionManager<TransferBill> getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager<TransferBill> transactionManager) {
        this.transactionManager = transactionManager;
    }

    public DtLogger<TransferBill> getDtLogger() {
        return dtLogger;
    }

    public void setDtLogger(DtLogger<TransferBill> dtLogger) {
        this.dtLogger = dtLogger;
    }
}
