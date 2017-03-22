package net.eric.tpc.biz;

import java.math.BigDecimal;

public class WithdrawMessage extends TransSystemMessage{
	private String bankCode;
	private String accountNumber;
	private BigDecimal amount;
	private String docNumber;
	private String transSN;
}
