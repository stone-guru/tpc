package net.eric.tpc.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Main {

	public static void main(String[] args) {
		TransStartRec transNodes = new TransStartRec("TR09282", new Node("localhost", 987),
				Arrays.asList(new Node("198.1.1.1", 10011), new Node("198.2.2.2", 10088)));
		String s = JSONObject.fromObject(transNodes).toString();
		System.out.println(s);
		
		JSONObject j = JSONObject.fromObject(s);
		TransStartRec nodes2 = (TransStartRec) JSONObject.toBean(j, TransStartRec.class);
		
		JSONArray array = JSONArray.fromObject(j.get("participants"));
		
		System.out.println(nodes2.toString());	
		System.out.println(array.toString());
		
		List<Node> nodes = new ArrayList<Node>();
		for(Iterator it = array.iterator(); it.hasNext(); ){
			Node n = (Node) JSONObject.toBean((JSONObject) it.next(), Node.class);
			nodes.add(n);
		}
		nodes2.setParticipants(nodes);
		
		for(Node n : nodes2.getParticipants()){
			System.out.println(n);
		}
	}
}
