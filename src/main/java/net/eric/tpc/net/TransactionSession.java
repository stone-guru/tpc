package net.eric.tpc.net;

import java.util.Properties;

public class TransactionSession extends Properties{

    private static final long serialVersionUID = 1L;
    
    private String xid;
    
    public TransactionSession(String xid){
        super();
        this.xid = xid;
    }
    
    
    public String xid() {
        return xid;
    }

}
