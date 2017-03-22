package net.eric.tpc.common;

public class Pair<T1, T2> {
	
	public static <A, B> Pair<A, B> asPair(A a, B b){
		return new Pair<A, B>(a, b);
	}
	
	private T1 fst;
	private T2 snd;
	
	public Pair(T1 fst, T2 snd){
		this.fst = fst;
		this.snd = snd;
	}
	
	public Pair(Pair<T1, T2> p){
		this.fst = p.fst;
		this.snd = p.snd;
	}
	
	public T1 fst(){
		return this.fst;
	}
	
	public T2 snd(){
		return this.snd;
	}
	
}
