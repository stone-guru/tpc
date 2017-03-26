package net.eric.tpc.bankserver.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Account {
    private String acctNumber;
    private String acctName;
    private BigDecimal balance;
    private String bankCode;
    private Date lastModiTime;
    private Date openingTime;
    private AccountType type;

    public String getAcctName() {
        return acctName;
    }

    public void setAcctName(String acctName) {
        this.acctName = acctName;
    }

    public String getAcctNumber() {
        return acctNumber;
    }

    public void setAcctNumber(String acctNumber) {
        this.acctNumber = acctNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public Date getLastModiTime() {
        return lastModiTime;
    }

    public void setLastModiTime(Date lastModiTime) {
        this.lastModiTime = lastModiTime;
    }

    public Date getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(Date openingTime) {
        this.openingTime = openingTime;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

}
