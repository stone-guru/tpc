package net.eric.tpc.persist;

public interface CoreDbInit {
   
    void dropDtTable();
    
    void createDtTable();
    
    void dropKeyTable();
    
    void createKeyTable();
}
