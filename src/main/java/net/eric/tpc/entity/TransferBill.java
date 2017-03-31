package net.eric.tpc.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.google.common.base.Strings;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.biz.BizCode;

public class TransferBill implements Serializable {

    private static final long serialVersionUID = -8098090408729937278L;

    private String transSN;
    private Date launchTime;
    private String receivingBankCode;
    private AccountIdentity account;
    private AccountIdentity oppositeAccount;
    private BigDecimal amount;
    private String voucherNumber;
    private String summary;

    public Date getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(Date launchTime) {
        this.launchTime = launchTime;
    }

    public String getReceivingBankCode() {
        return receivingBankCode;
    }

    public void setReceivingBankCode(String receivingBank) {
        this.receivingBankCode = receivingBank;
    }

    public AccountIdentity getAccount() {
        return account;
    }

    public void setAccount(AccountIdentity account) {
        this.account = account;
    }

    public AccountIdentity getOppositeAccount() {
        return oppositeAccount;
    }

    public void setOppositeAccount(AccountIdentity oppositeAccount) {
        this.oppositeAccount = oppositeAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTransSN() {
        return transSN;
    }

    public void setTransSN(String transSN) {
        this.transSN = transSN;
    }

    @Override
    public String toString() {
        return "TransferBill [transSN=" + transSN + ", launchTime=" + launchTime + ", receivingBankCode="
                + receivingBankCode + ", account=" + account + ", oppositeAccount=" + oppositeAccount + ", amount="
                + amount + ", voucherNumber=" + voucherNumber + ", summary=" + summary + "]";
    }

    public TransferBill copy() {
        TransferBill bill = new TransferBill();
        bill.transSN = this.transSN;
        bill.launchTime = this.launchTime;
        bill.receivingBankCode = this.receivingBankCode;
        bill.summary = this.summary;
        bill.amount = this.amount;
        bill.account = this.account.copy();
        bill.oppositeAccount = this.oppositeAccount.copy();
        bill.voucherNumber = this.voucherNumber;
        return bill;
    }

    public ActionStatus fieldCheck() {
        ActionStatus r = checkFieldMissing();
        if (!r.isOK()) {
            return r;
        }
        long now = new Date().getTime();
        if (this.getLaunchTime().getTime() > now) {
            return ActionStatus.create(BizCode.TIME_IS_FUTURE, this.getLaunchTime().toString());
        }

        if (this.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ActionStatus.create(BizCode.AMOUNT_LE_ZERO, this.getAmount().toString());
        }

        if (this.getAccount().equals(this.getOppositeAccount())) {
            return ActionStatus.create(BizCode.SAME_ACCOUNT, this.getAccount().toString());
        }

        if (this.getSummary() != null) {
            if (this.getSummary().length() > 48) {
                return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Summary");
            }
        }
        
        if(this.getTransSN().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "TransSn");
        }
        
        if(this.getReceivingBankCode().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "ReceivingBankCode");
        }
        
        if(this.getVoucherNumber().length() > 36){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "VoucherNumber");
        }
        
        if(this.getAccount().getNumber().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Account.Number");
        }
        
        if(this.getAccount().getBankCode().length() > 12){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "Account.BankCode");
        }

        if(this.getOppositeAccount().getNumber().length() > 24){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "OppositeAccount.Number");
        }
        
        if(this.getOppositeAccount().getBankCode().length() > 12){
            return ActionStatus.create(BizCode.FIELD_TOO_LONG, "OppositeAccount.BankCode");
        }
        
        return ActionStatus.OK;
    }

    private ActionStatus checkFieldMissing() {
        if (Strings.isNullOrEmpty(this.getTransSN())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "TransSN");
        }

        if (this.getLaunchTime() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "launchTime");
        }

        if (Strings.isNullOrEmpty(this.getReceivingBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "ReceivingBankCode");
        }

        if (Strings.isNullOrEmpty(this.getVoucherNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "VoucherNumber");
        }

        if (this.getAccount() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account");
        }

        if (Strings.isNullOrEmpty(this.getAccount().getBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account.BankCode");
        }

        if (Strings.isNullOrEmpty(this.getAccount().getNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Account.Number");
        }

        if (this.getOppositeAccount() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount");
        }

        if (Strings.isNullOrEmpty(this.getOppositeAccount().getBankCode())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount.BankCode");
        }

        if (Strings.isNullOrEmpty(this.getOppositeAccount().getNumber())) {
            return ActionStatus.create(BizCode.MISS_FIELD, "OppositeAccount.Number");
        }

        if (this.getAmount() == null) {
            return ActionStatus.create(BizCode.MISS_FIELD, "Amount");
        }

        return ActionStatus.OK;
    }

}
