package net.eric.tpc.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class Pair<T1, T2> implements Serializable {

    private static final long serialVersionUID = 2034602565707969200L;

    public static <A, B> Pair<A, B> asPair(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    public static <A, B> Optional<Pair<A,B>> of(A a, B b){
        return Optional.of(asPair(a, b));
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

    public static <A, B> List<A> projectFirst(Collection<Pair<A, B>> c) {
        Preconditions.checkNotNull(c, "collection");

        List<A> ax = new ArrayList<A>(c.size());
        for (Pair<A, B> p : c) {
            ax.add(p.fst());
        }
        return ax;
    }

    public static <A, B> List<B> projectSecond(Collection<Pair<A, B>> c) {
        Preconditions.checkNotNull(c, "collection");

        List<B> ax = new ArrayList<B>(c.size());
        for (Pair<A, B> p : c) {
            ax.add(p.snd());
        }
        return ax;
    }

    public static enum FieldTag{FIRST, SECOND};
    
    public static <A, B> boolean haskDuplicatedElement(List<Pair<A, B>> list, FieldTag field){
        for(int i = 0; i < list.size() - 1; i++){
            Pair<A, B> a = list.get(i);
            for(int j = i + 1; j < list.size(); j++){
                Pair<A, B> b = list.get(j);
                Object v1, v2;
                if(field == FieldTag.FIRST){
                    v1 = a.fst();
                    v2 = b.fst();
                }else{
                    v1 = a.snd();
                    v2 = b.snd();
                }
                if(Objects.equals(v1, v2)){
                    return true;
                }
            }
        }
        return false;
    }
    
    public static <A extends Comparable<? super A>, B extends Comparable<? super B>> Comparator<Pair<A, B>> newComparator() {
        return new Comparator<Pair<A, B>>() {
            @Override
            public int compare(Pair<A, B> o1, Pair<A, B> o2) {
                int cmp = o1.fst().compareTo(o2.fst);
                if (cmp != 0) {
                    return cmp;
                }
                return o1.snd().compareTo(o2.snd);
            }
        };
    }

    public static <A extends Comparable<? super A>, B> Comparator<Pair<A, B>> fstPartComparator() {
        return new Comparator<Pair<A, B>>() {
            @Override
            public int compare(Pair<A, B> o1, Pair<A, B> o2) {
                return o1.fst().compareTo(o2.fst);
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fst == null) ? 0 : fst.hashCode());
        result = prime * result + ((snd == null) ? 0 : snd.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (fst == null) {
            if (other.fst != null)
                return false;
        } else if (!fst.equals(other.fst))
            return false;
        if (snd == null) {
            if (other.snd != null)
                return false;
        } else if (!snd.equals(other.snd))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "<" + this.fst + ", " + this.snd + ">";
    }

}
