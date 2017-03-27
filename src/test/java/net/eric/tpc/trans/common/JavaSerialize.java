package net.eric.tpc.trans.common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Date;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.biz.AccountIdentity;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;

public class JavaSerialize {
    private static DataPacket genDataPacket(){
        TransferMessage msg = new TransferMessage();
        msg.setTransSN("982872393");
        msg.setTransType(TransferMessage.INCOME);
        msg.setLaunchTime(new Date());
        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(200));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");
        Node boc = new Node("localhost", 10021);
        Node bbc = new Node("localhost", 10022);
        TransStartRec st = new TransStartRec("KJDF000001", new Node("localhost", 9001),
                ImmutableList.of(boc, bbc));
        
        DataPacket packet = new DataPacket("BEGIN_TRANS", st, msg);
        return packet;
    }
    public static void main(String[] args) {
        
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("/tmp/my.out"));

            oos.writeObject(genDataPacket());
            oos.flush(); // 缓冲流
            oos.close(); // 关闭流
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
