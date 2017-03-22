package net.eric.tpc.biz;

import java.math.BigDecimal;

public class TransOutMessage extends TransSystemMessage{
	private String bankCode;
	private String accountNumber;
	private String destBankCode;
	private String destAccountNumber;
	private BigDecimal amount;
	private String docNumber;
}
