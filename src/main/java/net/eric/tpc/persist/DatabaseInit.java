package net.eric.tpc.persist;

public interface DatabaseInit {
    void createAccountTable();
    void createTransferMessageTable();
}
