package net.eric.tpc.proto;

public interface BizActionListener {
    
    void onSuccess(String xid);

    void onFailure(String xid);
}
