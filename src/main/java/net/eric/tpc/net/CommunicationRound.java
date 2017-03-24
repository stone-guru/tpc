package net.eric.tpc.net;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;

import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.Node;

public class CommunicationRound {
	private boolean beginRound = false;
	private boolean finishRound = false;

	private CommuResult result;
	private CountDownLatch latch;
	private int wantedCount;
	private ExecutorService pool;

	public CommunicationRound(ExecutorService pool) {
		this.pool = pool;
	}

	public int wantedCount() {
		return this.wantedCount;
	}

	synchronized public void startNewRound(int n) {
		this.result = new CommuResult(n);
		this.beginRound = true;
		this.finishRound = false;
		this.wantedCount = n;
		this.latch = new CountDownLatch(n);
		pool.submit(new Runnable() {
			public void run() {
				try {
					CommunicationRound.this.latch.await();
				} catch (InterruptedException e) {
					// Nothing should do here
				}
				System.out.println("round finished");
				finishRound = true;
			}
		});
	}

	synchronized public Future<CommuResult> getResult() {
		if (!this.isWithinRound()) {
			return Futures.immediateFuture(new CommuResult(CommunicationRound.this.result));
		}
		Callable<CommuResult> task = new Callable<CommuResult>() {
			public CommuResult call() throws Exception {
				// 超时控制由每一个网络动作处理，务必保证调用到regSuccess 或 regFailure，否这此处会一直阻塞
				CommunicationRound.this.latch.await();
				return new CommuResult(CommunicationRound.this.result);
			}
		};
		return pool.submit(task);
	}

	synchronized public boolean isWithinRound() {
		return this.beginRound && !this.finishRound;
	}

	synchronized public void regFailure(Node node, String errorCode, String errorReason) {
		if (!this.isWithinRound()) {
			return;
		}
		if (this.result.regFailure(node, errorCode, errorReason)) {
			this.latch.countDown();
		}
	}

	synchronized public void regSuccess(Node node, Object r) {
		if (!this.isWithinRound()) {
			return;
		}
		if (this.result.regSuccess(node, r)) {
			System.out.println("regSuccess to countDown");
			this.latch.countDown();
			System.out.println("regSuccess countDown ok");
		}
	}
}