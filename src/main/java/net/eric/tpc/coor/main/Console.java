package net.eric.tpc.coor.main;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.apache.mina.filter.codec.textline.TextLineCodecFactory;

import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.proto.CoorCommuResult;
import net.eric.tpc.proto.CoorCommunicator;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;

public class Console {
    public static void main(String[] args) throws Exception {
        CoorCommunicator communicator = new MinaCommunicator(new TextLineCodecFactory(Charset.forName("UTF-8")));
        TransStartRec transNodes = new TransStartRec("TR09282", new Node("localhost", 987),
                Arrays.asList(new Node("127.0.0.1", 10024), new Node("198.2.2.2", 10088)));
        Future<CoorCommuResult> task = null; //communicator.askBeginTrans(transNodes);
        
        CoorCommuResult result = (CoorCommuResult) task.get();
        
        System.out.println(result);

        communicator.shutdown();
    }
}
