package net.eric.tpc.coor.stub;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Configuration;
import net.eric.tpc.common.ErrorMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.proto.BizStrategy;
import net.eric.tpc.proto.Node;

public class AbcBizStrategy implements BizStrategy<TransferMessage> {
    private Configuration config;

    public List<Pair<Node, TransferMessage>> splitTask(TransferMessage b) {
        return null;
    }

    public Optional<ErrorMessage> checkTransRequest(TransferMessage m) {
        Optional<ErrorMessage> msg = checkFieldMissing(m);
        if(msg.isPresent()){
            return msg;
        }
        
        if(!config.isBankNodeExists(m.getAccount().getBankCode())){
            return Optional.of(ErrorMessage.create(BizErrorCode.NO_BANK_NODE, m.getAccount().getBankCode()));
        }
        
        if(!config.isBankNodeExists(m.getOppositeAccount().getBankCode())){
            return Optional.of(ErrorMessage.create(BizErrorCode.NO_BANK_NODE, m.getOppositeAccount().getBankCode()));
        }
        
        long now = new Date().getTime();
        if(m.getLaunchTime().getTime() > now){
            return Optional.of(ErrorMessage.create(BizErrorCode.TIME_IS_FUTURE,  m.getLaunchTime().toString()));
        }
        
        if(m.getAmount().compareTo(BigDecimal.ZERO) <= 0){
            return Optional.of(ErrorMessage.create(BizErrorCode.AMOUNT_LE_ZERO, m.getAmount().toString()));
        }

        if(m.getAccount().equals(m.getOppositeAccount())){
            return Optional.of(ErrorMessage.create(BizErrorCode.SAME_ACCOUNT, m.getAccount().toString()));
        }
        
        return Optional.absent();
    }

    Optional<ErrorMessage> checkFieldMissing(TransferMessage m) {
        if (m == null) {
            throw new NullPointerException("TransferMessage is null");
        }

        if (Strings.isNullOrEmpty(m.getTransSN())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "TransSN"));
        }

        if (m.getLaunchTime() == null) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "launchTime"));
        }

        if (Strings.isNullOrEmpty(m.getReceivingBankCode())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "ReceivingBankCode"));
        }

        if (Strings.isNullOrEmpty(m.getVoucherNumber())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "VoucherNumber"));
        }

        if (m.getAccount() == null) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "Account"));
        }

        if (Strings.isNullOrEmpty(m.getAccount().getBankCode())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "Account.BankCode"));
        }

        if (Strings.isNullOrEmpty(m.getAccount().getNumber())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "Account.Number"));
        }

        if (m.getOppositeAccount() == null) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "OppositeAccount"));
        }

        if (Strings.isNullOrEmpty(m.getOppositeAccount().getBankCode())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "OppositeAccount.BankCode"));
        }

        if (Strings.isNullOrEmpty(m.getOppositeAccount().getNumber())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "OppositeAccount.Number"));
        }

        if (m.getAmount() == null) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "Amount"));
        }

        if (m.getTransType() == null) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "TransType"));
        }

        if (Strings.isNullOrEmpty(m.getSummary())) {
            return Optional.of(ErrorMessage.create(BizErrorCode.MISS_FIELD, "Summary"));
        }

        return Optional.absent();
    }
}
