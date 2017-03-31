package net.eric.tpc.coor;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.proto.TransactionManager;

public class CoorIoHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CoorIoHandler.class);
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        DataPacket request = (DataPacket) message;
        DataPacket reponse;
        if (DataPacket.TRANS_BILL.equals(request.getCode())) {
            TransferBill bill = null;
            try {
                bill = (TransferBill) request.getParam1();
            } catch (Exception e) {
                // cast error
            }
            if (bill != null) {
                reponse = this.processBill(bill);
            } else {
                reponse = new DataPacket(DataPacket.TRANS_BILL_ANSWER, DataPacket.NO,
                        ActionStatus.create(DataPacket.BAD_DATA_PACKET, ""));
            }
        } else {
            reponse = new DataPacket(DataPacket.BAD_COMMNAD, DataPacket.NO,
                    new ActionStatus(BizCode.PEER_PRTC_ERROR, ""));
        }
        logger.info("ABC Server send reply command " + reponse.toString());
        session.write(reponse);
        
    }

    private DataPacket processBill(TransferBill bill) {
        ActionStatus actionStatus = this.transactionManager.transaction(bill);
        return DataPacket.fromActionStatus(DataPacket.TRANS_BILL_ANSWER, actionStatus);
    }

    public TransactionManager<TransferBill> getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager<TransferBill> transactionManager) {
        this.transactionManager = transactionManager;
    }

    private TransactionManager<TransferBill> transactionManager;
}
