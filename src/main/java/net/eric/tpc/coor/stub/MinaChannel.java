package net.eric.tpc.coor.stub;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.Executors;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransactionNodes;

public class MinaChannel {
	private SocketConnector connector;
	private ConnectFuture future;
	private IoSession session;

	public boolean connect(Node node) throws Exception {
		connector = new NioSocketConnector();
		connector.setConnectTimeoutMillis(3000);
		DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
		filterChain.addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
		connector.getSessionConfig().setUseReadOperation(true);
		future = connector.connect(new InetSocketAddress(node.getAddress(), node.getPort()));
		future.awaitUninterruptibly();
		session = future.getSession();

		return true;
	}

	public DataPacket request(DataPacket request) throws Exception {

		final String s = DataPacketCodec.encodeDataPacket(request);

		final WriteFuture writeFuture = session.write(s);
		writeFuture.awaitUninterruptibly();
		if (writeFuture.getException() != null) {
			return new DataPacket("ERROR", "Write error: " + writeFuture.getException().getMessage());
		}

		final ReadFuture readFuture = session.read();
		readFuture.awaitUninterruptibly();

		if (readFuture.getException() != null) {
			return new DataPacket("ERROR", "Read error: " + readFuture.getException().getMessage());
		}

		final String responseString = readFuture.getMessage().toString();
		return DataPacketCodec.decodeDataPacket(responseString);

	}

	public boolean close() throws Exception {
		CloseFuture future = session.getCloseFuture();
		future.awaitUninterruptibly(1000);
		connector.dispose();
		return true;
	}
}
