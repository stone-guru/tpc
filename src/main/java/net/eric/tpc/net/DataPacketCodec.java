package net.eric.tpc.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DataPacketCodec {
    public static String encodeTransStartRec(TransStartRec transNodes) {
        return JSONObject.fromObject(transNodes).toString();
    }

    public static TransStartRec decodeTransStartRec(String s) {
        JSONObject j = JSONObject.fromObject(s);
        TransStartRec transNodes = (TransStartRec) JSONObject.toBean(j, TransStartRec.class);

        JSONArray array = JSONArray.fromObject(j.get("participants"));

        List<Node> nodes = new ArrayList<Node>();
        @SuppressWarnings("rawtypes")
        Iterator it = array.iterator();
        while (it.hasNext()) {
            Node n = (Node) JSONObject.toBean((JSONObject) it.next(), Node.class);
            nodes.add(n);
        }
        transNodes.setParticipants(nodes);
        return transNodes;
    }

    public static String encodeTransferMessage(TransferMessage msg){
        return JSONObject.fromObject(msg).toString();
    }
    
    public static TransferMessage decodeTransferMessage(String s){
        JSONObject obj = JSONObject.fromObject(s);
        return (TransferMessage) JSONObject.toBean(obj, TransferMessage.class);
    }
    
    public static String encodeDataPacket(DataPacket packet) {
        return JSONObject.fromObject(packet).toString();
    }

    public static DataPacket decodeDataPacket(String s) {
        JSONObject obj = JSONObject.fromObject(s);
        return (DataPacket) JSONObject.toBean(obj, DataPacket.class);
    }
}
