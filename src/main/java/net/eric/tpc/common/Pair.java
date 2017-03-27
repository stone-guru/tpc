package net.eric.tpc.common;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

public class Pair<T1, T2>  implements Serializable{
    
    private static final long serialVersionUID = 2034602565707969200L;

    public static <A, B> List<A> keyList(List<Pair<A, B>> assoc){
        List<A> keys = Lists.newArrayList();
        for(Pair<A, B> p : assoc){
            keys.add(p.fst);
        }
        return keys;
    }
    
    
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
