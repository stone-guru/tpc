package net.eric.tpc.persist;

public interface DatabaseInit {
    void dropAccountTable();
    
    void createAccountTable();

    void dropTransferBillTable();
    
    void createTransferBillTable();
    
    void dropDtTable();
    
    void createDtTable();
}
