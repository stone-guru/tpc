package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CommuResult {

	private int wantedCount;
	private List<PeerResult> results;

	public CommuResult(int n) {
		this.wantedCount = n;
		this.results = Lists.newArrayList();
	}

	public CommuResult(CommuResult r) {
		this.wantedCount = r.wantedCount;
		this.results = ImmutableList.copyOf(r.results);
	}

	public CommuResult(int n, Iterable<PeerResult> results){
		this.wantedCount = n;
		this.results = ImmutableList.copyOf(results);
	}
	
	public int wantedCount() {
		return this.wantedCount;
	}

	public List<PeerResult> getResults() {
		return this.results;
	}

	public boolean isAllDone() {
		checkResultSize();
		return this.results.size() == wantedCount;
	}

	public boolean isAllOK() {
		checkResultSize();
		return isAllDone() && Iterables.all(this.results, CommuResult.IS_RIGHT);
	}

	public List<PeerResult> okResults(){
		return ImmutableList.copyOf(Iterables.filter(this.results, PeerResult.class));
	}
	
	public <T> List<T> okResultAs(Function<Object, T> f){
		List<T> list = new ArrayList<T>();
		for(PeerResult r : this.results){
			if(r.isRight()){
				list.add(f.apply(r.result()));
			}
		}
		return list;
	}
	
	public List<Node> getSuccessNodes(){
		List<Node> list = new ArrayList<Node>();
		for(PeerResult r : this.results){
			if(r.isRight()){
				list.add(r.peer());
			}
		}
		return list;
	}
	
//	public boolean regFailure(Node node, String errorCode, String errorReason) {
//		return this.regResult(new PeerResult(node, errorCode, errorReason));
//	}

//	public boolean regSuccess(Node node, Object r) {
//		assert (node != null);
//		assert (r != null);
//		return regResult(new PeerResult(node, r));
//	}
//
//	private boolean regResult(PeerResult r) {
//		if (this.isResultExists(r.peer())) {
//			return false;
//		}
//		this.results.add(r);
//		return true;
//	}
//
//	private boolean isResultExists(Node node) {
//		for (PeerResult r : this.results) {
//			if (r.peer().equals(node)) {
//				return true;
//			}
//		}
//		return false;
//	}

	private void checkResultSize() {
		if (this.results.size() > wantedCount) {
			throw new IllegalStateException("Inner error, count of result great than wanted count");
		}
	}

	public static Predicate<PeerResult> IS_RIGHT = new Predicate<PeerResult>() {
		public boolean apply(PeerResult r) {
			return r.isRight();
		}
	};

	public static class PeerResult {
		private boolean hasError;
		private boolean isDone;
		private String errorCode;
		private String errorReason;
		private Object result;
		private Node peer;

		public PeerResult(Node peer, Object result){
			this(peer, result, true);
		}
		
		public PeerResult(Node peer, Object result, boolean isDone) {
			assert (peer != null);
			if (result == null) {
				throw new NullPointerException("result can not be null");
			}

			this.hasError = false;
			this.isDone = isDone;
			this.peer = peer;
			this.result = result;
		}

		public PeerResult(Node peer, String errorCode, String errorReason) {
			if (errorCode == null) {
				throw new NullPointerException("errorCode can not be null");
			}

			this.hasError = true;
			this.isDone = true;
			this.peer = peer;
			this.errorCode = errorCode;
			this.errorReason = errorReason;
		}

		public PeerResult asDone(){
			if(this.hasError){
				throw new IllegalStateException("PeerResult contain error, can not be regard as Done");
			}
			if(this.isDone){
				return this;
			}
			return new PeerResult(this.peer, this.result);
		}
		
		public Node peer() {
			return this.peer;
		}

		public boolean isDone(){
			return this.isDone;
		}
		
		public boolean isRight() {
			return !this.hasError && this.isDone;
		}

		public String errorCode() {
			if (!this.hasError) {
				throw new IllegalStateException("This CommuResult is right, has no errorCode");
			}
			return this.errorCode;
		}

		public String errorReason() {
			if (!this.hasError) {
				throw new IllegalStateException("This CommuResult is right, has no errorReason");
			}
			return this.errorReason;
		}

		public Object result() {
			if (this.hasError) {
				throw new IllegalStateException("This CommuResult is not right, has no result");
			}
			return this.result;
		}
	}
}
