package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.net.PeerResult;

public class RoundResult {
    
    public static ActionStatus force(Future<RoundResult> future) {
        try {
            RoundResult result = future.get();
            if (result.isAllOK()) {
                return ActionStatus.OK;
            } else {
                return result.getAnError();
            }
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        }
    }

    private int wantedCount;
    private List<PeerResult> results;

    public RoundResult(int n) {
        this.wantedCount = n;
        this.results = Lists.newArrayList();
    }

    public RoundResult(RoundResult r) {
        this.wantedCount = r.wantedCount;
        this.results = ImmutableList.copyOf(r.results);
    }

    public RoundResult(int n, Iterable<PeerResult> results) {
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
        return isAllDone() && Iterables.all(this.results, RoundResult.IS_RIGHT);
    }

    public List<PeerResult> okResults() {
        return ImmutableList.copyOf(Iterables.filter(this.results, PeerResult.class));
    }

    public <T> Optional<T> getAnOkResult(Class<T> c){
        for(PeerResult r : this.results){
            if(r.isRight()){
                if(!c.isInstance(r.result())){
                    throw new ClassCastException("result can not cast to given type " + c.getName());
                }
                
                @SuppressWarnings("unchecked")
                final T t = (T)r.result();
                return Optional.of(t);
            }
        }
        return Optional.absent();
    }
    
    public int okResultCount(){
        int i = 0;
        for(PeerResult r : this.results){
            if(r.isRight()){
                i++;
            }
        }
        return i;
    }
    
    public <T> List<T> okResultAs(Function<Object, T> f) {
        List<T> list = new ArrayList<T>();
        for (PeerResult r : this.results) {
            if (r.isRight()) {
                list.add(f.apply(r.result()));
            }
        }
        return list;
    }

    public ActionStatus getAnError() {
        if (!isAllDone()) {
            return ActionStatus.PEER_NO_REPLY;
        }
        for (PeerResult r : this.results) {
            if (!r.isRight()) {
                return r.errorMessage();
            }
        }
        return ActionStatus.INNER_ERROR;
    }

    public List<InetSocketAddress> getSuccessNodes() {
        List<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
        for (PeerResult r : this.results) {
            if (r.isRight()) {
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

    public static Predicate<PeerResult> IS_RIGHT = new Predicate<PeerResult>() {
        public boolean apply(PeerResult r) {
            return r.isRight();
        }
    };
}
