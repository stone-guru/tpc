package net.eric.tpc.biz;

import java.math.BigDecimal;

public class TransInMessage extends TransSystemMessage {
	private String bankCode;
	private String accountNumber;
	private String sourceBankCode;
	private String sourceAccountNumber;
	private BigDecimal amount;
	private String docNumber;

}
