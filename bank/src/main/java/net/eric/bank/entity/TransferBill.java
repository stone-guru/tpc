package net.eric.bank.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.google.common.base.Strings;

public class TransferBill implements Serializable {

    private static final long serialVersionUID = -8098090408729937278L;

    private String transSN;
    private Date launchTime;
    private String receivingBankCode;
    private AccountIdentity payer;
    private AccountIdentity receiver;
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

    public AccountIdentity getPayer() {
        return payer;
    }

    public void setPayer(AccountIdentity payer) {
        this.payer = payer;
    }

    public AccountIdentity getReceiver() {
        return receiver;
    }

    public void setReceiver(AccountIdentity receiver) {
        this.receiver = receiver;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVoucherNumber() {
        return Strings.nullToEmpty(voucherNumber);
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public String getSummary() {
        return Strings.nullToEmpty(summary);
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
                + receivingBankCode + ", Payer=" + payer + ", Receiver=" + receiver + ", amount="
                + amount + ", voucherNumber=" + voucherNumber + ", summary=" + summary + "]";
    }

    public TransferBill copy() {
        TransferBill bill = new TransferBill();
        bill.transSN = this.transSN;
        bill.launchTime = this.launchTime;
        bill.receivingBankCode = this.receivingBankCode;
        bill.summary = this.summary;
        bill.amount = this.amount;
        bill.payer = this.payer.copy();
        bill.receiver = this.receiver.copy();
        bill.voucherNumber = this.voucherNumber;
        return bill;
    }

}
