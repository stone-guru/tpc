package net.eric.tpc.coor;

import java.math.BigDecimal;
import java.util.Date;

public class Transfer {
	private String serialNumber;
	private String transOutBankCode;
	private String transOutAccountNumber;
	private String transInBankCode;
	private String transInAccountNumber;
	private BigDecimal amount;
	private Date   applyTime;
	private Date   handleTime;
	private String docNumber;
	
	public String getTransOutBankCode() {
		return transOutBankCode;
	}
	public void setTransOutBankCode(String transOutBankCode) {
		this.transOutBankCode = transOutBankCode;
	}
	public String getTransOutAccountNumber() {
		return transOutAccountNumber;
	}
	public void setTransOutAccountNumber(String transOutAccountNumber) {
		this.transOutAccountNumber = transOutAccountNumber;
	}
	public String getTransInBankCode() {
		return transInBankCode;
	}
	public void setTransInBankCode(String transInBankCode) {
		this.transInBankCode = transInBankCode;
	}
	public String getTransInAccountNumber() {
		return transInAccountNumber;
	}
	public void setTransInAccountNumber(String transInAccountNumber) {
		this.transInAccountNumber = transInAccountNumber;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public Date getApplyTime() {
		return applyTime;
	}
	public void setApplyTime(Date applyTime) {
		this.applyTime = applyTime;
	}
	public Date getHandleTime() {
		return handleTime;
	}
	public void setHandleTime(Date handleTime) {
		this.handleTime = handleTime;
	}
	public String getDocNumber() {
		return docNumber;
	}
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
}
