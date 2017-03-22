package net.eric.tpc.bankserver.persist;

public interface DatabaseInit {
	void createAccountTable();
	void createDepositMessageTable();
}
