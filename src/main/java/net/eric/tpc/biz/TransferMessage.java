package net.eric.tpc.biz;

import java.math.BigDecimal;
import java.util.Date;

public class TransferMessage {
	private String messageNumber;
	private Date launchTime;
	private String version;
	private String sender;
	private String receiver;
	private MessageType messageType;
	private String receivingBankCode;
	private AccountIdentity account;
	private AccountIdentity oppositeAccount;
	private TransType transType;
	private String transSN;
	private BigDecimal amount;
	private String voucherNumber;
	private String summary;

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType type) {
		this.messageType = type;
	}

	public String getMessageNumber() {
		return messageNumber;
	}

	public void setMessageNumber(String messageNumber) {
		this.messageNumber = messageNumber;
	}

	public Date getLaunchTime() {
		return launchTime;
	}

	public void setLaunchTime(Date launchTime) {
		this.launchTime = launchTime;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
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

}
