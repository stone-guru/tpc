package net.eric.tpc.proto;

public interface BizActionListener {
    
    void onSuccess(long xid);

    void onFailure(long xid);
}
