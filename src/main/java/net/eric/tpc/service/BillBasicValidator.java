package net.eric.tpc.service;

import java.math.BigDecimal;
import java.util.Date;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;

public class BillBasicValidator implements Validator<TransferBill> {

    @Override
    public ActionStatus validate(TransferBill bill) {
        Preconditions.checkNotNull(bill);
        ActionStatus r = checkFieldMissing(bill);
        if (!r.isOK()) {
            return r;
        }
        return this.fieldCheck(bill);
    }


    public ActionStatus fieldCheck(TransferBill bill) {
        long now = new Date().getTime();
        if (bill.getLaunchTime().getTime() > now) {
            return ActionStatus.create(BizCode.TIME_IS_FUTURE, bill.getLaunchTime().toString());
        }

        if (bill.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ActionStatus.create(BizCode.AMOUNT_LE_ZERO, bill.getAmount().toString());
        }

        if (bill.getPayer().equals(bill.getReceiver())) {
            return ActionStatus.create(BizCode.SAME_ACCOUNT, bill.getPayer().toString());
        }

        if (bill.getSummary() != null) {
            if (bill.getSummary().length() > 48) {
                return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Summary");
            }
        }
        
        if(bill.getTransSN().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "TransSn");
        }
        
        if(bill.getReceivingBankCode().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "ReceivingBankCode");
        }
        
        if(bill.getVoucherNumber().length() > 36){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "VoucherNumber");
        }
        
        if(bill.getPayer().getNumber().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Account.Number");
        }
        
        if(bill.getPayer().getBankCode().length() > 12){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Account.BankCode");
        }

        if(bill.getReceiver().getNumber().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "OppositeAccount.Number");
        }
        
        if(bill.getReceiver().getBankCode().length() > 12){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "OppositeAccount.BankCode");
        }
        
        if(bill.getAmount().precision() > 3){
            return ActionStatus.create(BizCode.AMOUNT_PRECISION_ERROR,  "precision of amount can not great than 2");
        }
        
        return ActionStatus.OK;
    }

    private ActionStatus checkFieldMissing(TransferBill bill) {
        if (Strings.isNullOrEmpty(bill.getTransSN())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "TransSN");
        }

        if (bill.getLaunchTime() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "launchTime");
        }

        if (Strings.isNullOrEmpty(bill.getReceivingBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "ReceivingBankCode");
        }

        if (Strings.isNullOrEmpty(bill.getVoucherNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "VoucherNumber");
        }

        if (bill.getPayer() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account");
        }

        if (Strings.isNullOrEmpty(bill.getPayer().getBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account.BankCode");
        }

        if (Strings.isNullOrEmpty(bill.getPayer().getNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account.Number");
        }

        if (bill.getReceiver() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount");
        }

        if (Strings.isNullOrEmpty(bill.getReceiver().getBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount.BankCode");
        }

        if (Strings.isNullOrEmpty(bill.getReceiver().getNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount.Number");
        }

        if (bill.getAmount() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Amount");
        }

        return ActionStatus.OK;
    }

}
