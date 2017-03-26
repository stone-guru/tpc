package net.eric.tpc.net;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.proto.CommuResult;
import net.eric.tpc.proto.CommuResult.PeerResult;
import net.eric.tpc.proto.Node;

public class CommunicationRound {
	public static enum RoundType {
		DOUBLE_SIDE, SINGLE_SIDE
	};

	public static interface MessageAssembler {
		PeerResult firstMessage(Node node, Object message);

		PeerResult fold(Node node, PeerResult previous, Object message);

		PeerResult transEnd(Node node, Optional<PeerResult> previous);
	}

	private boolean beginRound = false;
	private boolean finishRound = false;
	private RoundType roundType = RoundType.DOUBLE_SIDE;

	private CountDownHolder latch;
	private int wantedCount;
	private ExecutorService pool;
	private MessageAssembler assembler;
	private ConcurrentMap<Node, PeerResult> resultMap = new ConcurrentHashMap<Node, PeerResult>();

	public CommunicationRound(ExecutorService pool, int n, RoundType roundType, MessageAssembler assembler) {
		this.pool = pool;
		this.beginRound = true;
		this.finishRound = false;
		this.wantedCount = n;
		this.roundType = roundType;
		this.assembler = assembler;

		this.latch = new CountDownHolder(n);
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

	public CommunicationRound(ExecutorService pool, int n, RoundType roundType) {
		this(pool, n, roundType, CommunicationRound.ONE_ITEM_ASSEMBLER);
	}

	public RoundType roundType() {
		return this.roundType;
	}

	public int wantedCount() {
		return this.wantedCount;
	}

	synchronized public Future<CommuResult> getResult() {
		if (!this.isWithinRound()) {
			return Futures.immediateFuture(this.generateResult());
		}
		Callable<CommuResult> task = new Callable<CommuResult>() {
			public CommuResult call() throws Exception {
				// 超时控制由每一个网络动作处理，务必保证调用到regMessage 或 regFailure，否这此处会一直阻塞
				CommunicationRound.this.latch.await();
				return generateResult();
			}
		};
		return pool.submit(task);
	}

	public void clearLatch(){
		this.latch.clear();
	}
	
	private CommuResult generateResult() {
		return new CommuResult(this.wantedCount, this.resultMap.values());
	}

	synchronized public boolean isWithinRound() {
		return this.beginRound && !this.finishRound;
	}

	synchronized public void regFailure(Node node, String errorCode, String errorReason) {
		if (!this.isWithinRound() || this.isPeerResultDone(node)) {
			return;
		}
		this.resultMap.put(node, new PeerResult(node, errorCode, errorReason));
		this.latch.countDown(node);
	}

	public void regMessage(Node node, Object message) {
		regMessage(node, Optional.of(message));
	}

	public void connectionClosed(Node node) {
		regMessage(node, Optional.absent());
	}

	private Optional<PeerResult> tryAddMeesageforNewNode(Node node, Optional<Object> r) {
		PeerResult result = null;
		synchronized (this.resultMap) {
			if (!this.resultMap.containsKey(node)) {
				if (r.isPresent()) {
					result = this.assembler.firstMessage(node, r.get());
				} else {
					final Optional<PeerResult> none = Optional.absent();
					result = this.assembler.transEnd(node, none);
				}
				this.resultMap.put(node, result);
				return Optional.of(result);
			}
		}
		return Optional.absent();
	}

	private void regMessage(Node node, Optional<Object> r) {
		if (!this.isWithinRound() || this.isPeerResultDone(node)) {
			return;
		}
		Optional<PeerResult> opt = Optional.absent();
		if (!this.resultMap.containsKey(node)) {
			opt = tryAddMeesageforNewNode(node, r);
		}
		if (!opt.isPresent()) {
			if (r.isPresent()) {
				final PeerResult tmp = new PeerResult(node, r.get(), true);
				opt = Optional.of(this.resultMap.merge(node, tmp, CommunicationRound.this.FOLD_FUNCTION));
			} else {
				final PeerResult tmp = new PeerResult(node, new Object(), true);
				opt = Optional.of(this.resultMap.merge(node, tmp, CommunicationRound.this.END_FUNCTION));
			}
		}

		if (opt.get().isDone()) {
			System.out.println("regSuccess to countDown");
			this.latch.countDown(node);
			System.out.println("regSuccess countDown ok");
		}
	}

	private boolean isPeerResultDone(Node node) {
		if (this.resultMap.containsKey(node)) {
			PeerResult r = this.resultMap.get(node);
			return r.isDone();
		}
		return false;
	}

	private static class CountDownHolder {
		private CountDownLatch countDown;
		private Set<Node> callerSet;

		public CountDownHolder(int n) {
			countDown = new CountDownLatch(n);
			callerSet = new HashSet<Node>();
		}

		synchronized public void countDown(Node node) {
			if (!this.callerSet.contains(node)) {
				this.callerSet.add(node);
				this.countDown.countDown();
			}
		}

		public void await() throws InterruptedException {
			this.countDown.await();
		}

		public void clear() {
			while (this.countDown.getCount() > 0) {
				this.countDown.countDown();
			}
		}
	}

	public final BiFunction<PeerResult, PeerResult, PeerResult> FOLD_FUNCTION = new BiFunction<PeerResult, PeerResult, PeerResult>() {
		public PeerResult apply(PeerResult t, PeerResult u) {
			return CommunicationRound.this.assembler.fold(t.peer(), t, u.result());
		}
	};

	public final BiFunction<PeerResult, PeerResult, PeerResult> END_FUNCTION = new BiFunction<PeerResult, PeerResult, PeerResult>() {
		public PeerResult apply(PeerResult t, PeerResult u) {
			return CommunicationRound.this.assembler.transEnd(u.peer(), Optional.of(t));
		}
	};

	public static abstract class OneItemAssembler implements MessageAssembler {
		@Override
		public PeerResult fold(Node node, PeerResult previous, Object message) {
			return previous;
		}

		@Override
		public PeerResult transEnd(Node node, Optional<PeerResult> previous) {
			if (previous.isPresent()) {
				return previous.get();
			} else {
				return new PeerResult(node, "NO DATA", "No data received");
			}
		}
	}
	
	public static final MessageAssembler ONE_ITEM_ASSEMBLER = new OneItemAssembler() {
		@Override
		public PeerResult firstMessage(Node node, Object message) {
			return new PeerResult(node, message);
		}
	};
}