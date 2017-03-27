package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.eric.tpc.common.ActionResult;
import net.eric.tpc.net.PeerResult2;

public class CoorCommuResult {

    private int wantedCount;
    private List<PeerResult2> results;

    public CoorCommuResult(int n) {
        this.wantedCount = n;
        this.results = Lists.newArrayList();
    }

    public CoorCommuResult(CoorCommuResult r) {
        this.wantedCount = r.wantedCount;
        this.results = ImmutableList.copyOf(r.results);
    }

    public CoorCommuResult(int n, Iterable<PeerResult2> results){
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
        return isAllDone() && Iterables.all(this.results, CoorCommuResult.IS_RIGHT);
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
    
    public ActionResult getAnError(){
        if(!isAllDone()){
            return ActionResult.PEER_NO_REPLY;
        }
        for(PeerResult2 r : this.results){
            if(!r.isRight()){
               return r.errorMessage();
            }
        }
        return ActionResult.INNER_ERROR;
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
