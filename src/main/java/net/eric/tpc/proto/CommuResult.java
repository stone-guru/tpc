package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.eric.tpc.net.PeerResult2;

public class CommuResult {

    private int wantedCount;
    private List<PeerResult2> results;

    public CommuResult(int n) {
        this.wantedCount = n;
        this.results = Lists.newArrayList();
    }

    public CommuResult(CommuResult r) {
        this.wantedCount = r.wantedCount;
        this.results = ImmutableList.copyOf(r.results);
    }

    public CommuResult(int n, Iterable<PeerResult2> results){
        this.wantedCount = n;
        this.results = ImmutableList.copyOf(results);
    }
    
    public int wantedCount() {
        return this.wantedCount;
    }

    public List<PeerResult2> getResults() {
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

    public List<PeerResult2> okResults(){
        return ImmutableList.copyOf(Iterables.filter(this.results, PeerResult2.class));
    }
    
    public <T> List<T> okResultAs(Function<Object, T> f){
        List<T> list = new ArrayList<T>();
        for(PeerResult2 r : this.results){
            if(r.isRight()){
                list.add(f.apply(r.result()));
            }
        }
        return list;
    }
    
    public List<Node> getSuccessNodes(){
        List<Node> list = new ArrayList<Node>();
        for(PeerResult2 r : this.results){
            if(r.isRight()){
                list.add(r.peer());
            }
        }
        return list;
    }
    
//    public boolean regFailure(Node node, String errorCode, String errorReason) {
//        return this.regResult(new PeerResult2(node, errorCode, errorReason));
//    }

//    public boolean regSuccess(Node node, Object r) {
//        assert (node != null);
//        assert (r != null);
//        return regResult(new PeerResult2(node, r));
//    }
//
//    private boolean regResult(PeerResult2 r) {
//        if (this.isResultExists(r.peer())) {
//            return false;
//        }
//        this.results.add(r);
//        return true;
//    }
//
//    private boolean isResultExists(Node node) {
//        for (PeerResult2 r : this.results) {
//            if (r.peer().equals(node)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private void checkResultSize() {
        if (this.results.size() > wantedCount) {
            throw new IllegalStateException("Inner error, count of result great than wanted count");
        }
    }

    public static Predicate<PeerResult2> IS_RIGHT = new Predicate<PeerResult2>() {
        public boolean apply(PeerResult2 r) {
            return r.isRight();
        }
    };
}
