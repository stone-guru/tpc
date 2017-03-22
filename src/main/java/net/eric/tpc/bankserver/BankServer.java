package net.eric.tpc.bankserver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BankServer {
	public static void main(String[] args){
		final int portNumber = 18001;
		try{
			ServerSocket socket = new ServerSocket(portNumber);
			Socket clientSocket = socket.accept();
			
			InputStream is = clientSocket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader reader = new BufferedReader(isr);
			
			while(true){
				String req = reader.readLine();
				System.out.println("Request: " + req);
				if(req == null || "BYE".equals(req.toUpperCase()))
					break;
			}
			
			clientSocket.close();
			socket.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
