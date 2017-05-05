package net.eric.bank.coor;

import net.eric.bank.biz.BankCommandCodes;
import net.eric.bank.entity.TransferBill;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.net.CommandCodes;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.TransSession;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.TransactionManager;

import javax.inject.Inject;

/**
 * Created by bison on 5/5/17.
 */
public class TransferBillRequestHandler implements RequestHandler {

    @Inject
    private TransactionManager<TransferBill> transactionManager;

    @Override
    public short[] getCorrespondingCodes() {
        return new short[]{BankCommandCodes.TRANS_BILL};
    }

    @Override
    public ProcessResult process(TransSession session, Message request) {
        if (request.getParam() instanceof TransferBill) {
            TransferBill bill = (TransferBill) request.getParam();
            ActionStatus st = this.transactionManager.transaction(bill);
            Message response = Message.fromRequest(request, st.isOK() ? CommandCodes.YES : CommandCodes.NO, st);
            return new ProcessResult(response, false);
        }
        return ProcessResult.errorAndClose(request.getXid(), request.getRound());
    }
}
