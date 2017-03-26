package net.eric.tpc.coor.stub;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import net.eric.tpc.biz.TransferMessage;

public class TransServiceStub {
    private Socket socket ;

    public boolean connect() throws Exception {
         socket = new Socket("localhost", 10024);
         return true;
    }

    public String request(TransferMessage msg) throws Exception {
        OutputStream os = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(os);
        
        InputStream is = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        writer.println("hello");
        writer.flush();
        String s = reader.readLine();
        
        return s;
    }

    public boolean close() throws Exception {
        socket.close();
        return true;
    }
}
