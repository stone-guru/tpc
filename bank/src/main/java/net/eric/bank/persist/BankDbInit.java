package net.eric.bank.persist;

public interface BankDbInit {
    void dropAccountTable();
    
    void createAccountTable();

    void dropTransferBillTable();
    
    void createTransferBillTable();
}
