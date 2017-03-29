package net.eric.tpc.common;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Pair<T1, T2> implements Serializable {

    private static final long serialVersionUID = 2034602565707969200L;

    public static <A, B> List<A> keyList(List<Pair<A, B>> assoc) {
        Preconditions.checkNotNull(assoc);

        List<A> keys = Lists.newArrayList();
        for (Pair<A, B> p : assoc) {
            keys.add(p.fst);
        }
        return keys;
    }

    public static <A, B> Pair<A, B> asPair(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    public static <T> Pair<T, T> fromIterator(Iterator<T> it) {
        Preconditions.checkNotNull(it);

        if (it.hasNext()) {
            T one = it.next();
            if (it.hasNext()) {
                return Pair.asPair(one, it.next());
            }
            throw new IllegalArgumentException("Given iterator has only 1 elem");
        }
        throw new IllegalArgumentException("Given iterator has no elem");
    }

    public static <A extends Comparable<? super A>, B extends Comparable<? super B>> Comparator<Pair<A, B>> newComparator() {
        return new Comparator<Pair<A, B>>() {
            @Override
            public int compare(Pair<A, B> o1, Pair<A, B> o2) {
                if (o1.fst().compareTo(o2.fst) == 0) {
                    return 0;
                }
                return o1.snd().compareTo(o2.snd);
            }
        };
    }

    private T1 fst;
    private T2 snd;

    public Pair(T1 fst, T2 snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public Pair(Pair<T1, T2> p) {
        this.fst = p.fst;
        this.snd = p.snd;
    }

    public T1 fst() {
        return this.fst;
    }

    public T2 snd() {
        return this.snd;
    }

}
