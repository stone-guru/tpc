package net.eric.tpc.biz;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class TransferMessage implements Serializable {

    private static final long serialVersionUID = -8098090408729937278L;
   
    
    private String transSN;
    private Date launchTime;
    private String receivingBankCode;
    private AccountIdentity account;
    private AccountIdentity oppositeAccount;
    private TransType transType;
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

    public TransType getTransType() {
        return transType;
    }

    public void setTransType(TransType transType) {
        this.transType = transType;
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
        return "TransferMessage [transSN=" + transSN + ", launchTime=" + launchTime + ", receivingBankCode="
                + receivingBankCode + ", account=" + account + ", oppositeAccount=" + oppositeAccount + ", transType="
                + transType + ", amount=" + amount + ", voucherNumber=" + voucherNumber + ", summary=" + summary + "]";
    }

    
}
