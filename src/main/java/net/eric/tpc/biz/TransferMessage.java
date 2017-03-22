package net.eric.tpc.biz;

import java.math.BigDecimal;

public class TransferMessage extends TransSystemMessage {
	private String destBankCode;
	private String destAccountNumber;
	private String sourceBankCode;
	private String sourceAccountNumber;
	private BigDecimal amount;
	private String docNumber;
}
