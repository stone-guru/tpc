package net.eric.bank.bod;

import static net.eric.tpc.base.Pair.asPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.eric.bank.persist.AccountDao;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.ShouldNotHappenException;

public class AccountLocker {
    private Map<String, Long> keyMap = new HashMap<String, Long>();
    private int tryTimes;
    private int sleepMilis;
    @Inject
    private AccountDao accountDao;

    public AccountLocker() {
        this(3, 500);
    }

    public AccountLocker(int tryTimes, int sleepMilis) {
        this.tryTimes = tryTimes;
        this.sleepMilis = sleepMilis;
    }

    public boolean repeatTryLock(String accountNumber, String oppAccountNumber, long xid) {
        int i = 0;
        do {
            boolean locked = this.tryLock(accountNumber, oppAccountNumber, xid);
            if (locked) {
                return true;
            }
            try {
                Thread.sleep(sleepMilis);
            } catch (InterruptedException e) {
                return false;
            }
            i++;
        } while (i < tryTimes);
        return false;
    }

    synchronized public boolean tryLock(String key1, String key2, long xid) {
        if (this.containAny(key1, key2)) {
            return false;
        }

        keyMap.put(key1, xid);
        keyMap.put(key2, xid);
        this.accountDao.updateLock(Pair.asPair(key1, xid));
        this.accountDao.updateLock(Pair.asPair(key2, xid));
        return true;
    }

    synchronized public void releaseLock(String key1, String key2, long xid) {
        if (!this.containBoth(key1, key2)) {
            throw new ShouldNotHappenException();
        }
        keyMap.remove(key1);
        keyMap.remove(key2);

        final Long nullXid = null;
        accountDao.updateLock(asPair(key1, nullXid));
        accountDao.updateLock(asPair(key2, nullXid));
    }

    public boolean areBothLockedCorrectlly(String key1, String key2, long xid) {
        if (!this.containBoth(key1, key2)) {
            return false;
        }
        if (!Objects.equal(xid, keyMap.get(key1))) {
            return false;
        }
        if (!Objects.equal(xid, keyMap.get(key2))) {
            return false;
        }
        return true;
    }

    synchronized public ActionStatus releaseByXid(long xid) {
        Maybe<Pair<String, String>> maybe = this.getLockedKeyByXid(xid);
        if (!maybe.isRight()) {
            return maybe.getLeft();
        }
        Pair<String, String> doubleKey = maybe.getRight();

        this.releaseLock(doubleKey.fst(), doubleKey.snd(), xid);
        return ActionStatus.OK;
    }

    synchronized public Maybe<Pair<String, String>> getLockedKeyByXid(long xid) {
        final List<String> keys = new ArrayList<String>(2);
        for (String k : keyMap.keySet()) {
            if (Objects.equal(xid, keyMap.get(k))) {
                keys.add(k);
            }
        }
        if (keys.size() == 2) {
            return Maybe.success(Pair.fromIterator(keys.iterator()));
        }
        if (keys.size() == 0) {
            return Maybe.fail(ActionStatus.innerError("no locked key for trans " + xid));
        } else {
            throw new ShouldNotHappenException("releaseByXid num of locked key is " + keys.size());
        }
    }

    public void setLockedKey(List<Pair<String, Long>> keys) {
        Preconditions.checkNotNull(keys, "Given keyset is null");
        assureKeyRule(keys);
        synchronized (this) {
            keyMap.clear();
            for (Pair<String, Long> p : keys) {
                keyMap.put(p.fst(), p.snd());
            }
        }
    }

    private void assureKeyRule(List<Pair<String, Long>> keys) {
        @SuppressWarnings("unchecked")
        Pair<String, Long>[] sortedKeys = keys.toArray(new Pair[keys.size()]);
        final Comparator<Pair<String, Long>> c = Pair.newComparator();
        Arrays.sort(sortedKeys, c);

        Map<Long, Integer> xidMap = new HashMap<Long, Integer>();
        String prevKey = "";
        for (Pair<String, Long> p : sortedKeys) {
            // 排序过后相同的key将排在一起
            if (p.fst().equals(prevKey)) {
                throw new IllegalArgumentException(prevKey + "occurs more than once");
            }
            Long xid = p.snd();
            int v = 0;
            if (xidMap.containsKey(xid)) {
                v = xidMap.get(xid);
            }
            if (v >= 2) {
                throw new IllegalArgumentException("xid " + xid + " locked by more than 2 key");
            }
            xidMap.put(xid, v + 1);

            prevKey = p.fst();
        }

        for (Long xid : xidMap.keySet()) {
            int v = xidMap.get(xid);
            if (v < 2) {
                throw new IllegalArgumentException("xid " + xid + " locked by only 1 key");
            }
        }
    }

    private boolean containAny(String key1, String key2) {
        if (keyMap.containsKey(key1) || keyMap.containsKey(key2)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean containBoth(String key1, String key2) {
        if (keyMap.containsKey(key1) && keyMap.containsKey(key2)) {
            return true;
        } else {
            return false;
        }
    }

    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }
}