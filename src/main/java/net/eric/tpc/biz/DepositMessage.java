package net.eric.tpc.biz;

import java.math.BigDecimal;

public class DepositMessage extends TransSystemMessage{
	private String bankCode;
	private String accountNumber;
	private BigDecimal amount;
	private String docNumber;
	private String transSN;
	
	public String getBankCode() {
		return bankCode;
	}
	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public String getDocNumber() {
		return docNumber;
	}
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}
	public String getTransSN() {
		return transSN;
	}
	public void setTransSN(String transSN) {
		this.transSN = transSN;
	}

}
