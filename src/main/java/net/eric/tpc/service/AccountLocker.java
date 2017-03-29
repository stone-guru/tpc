package net.eric.tpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Objects;

import net.eric.tpc.common.Pair;
import net.eric.tpc.common.ShouldNotHappenException;
import net.eric.tpc.persist.AccountDao;

public class AccountLocker {
    private ConcurrentHashMap<String, String> keyMap = new ConcurrentHashMap<String, String>();
    private int tryTimes;
    private int sleepMilis;
    private AccountDao accountDao;

    public AccountLocker(int tryTimes, int sleepMilis) {
        this.tryTimes = tryTimes;
        this.sleepMilis = sleepMilis;
    }

    public boolean repeatTryLock(String accountNumber, String oppAccountNumber, String xid) {
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

    synchronized public boolean tryLock(String key1, String key2, String xid) {
        if (this.containAny(key1, key2)) {
            return false;
        }

        keyMap.put(key1, xid);
        keyMap.put(key2, xid);
        this.accountDao.updateLock(Pair.asPair(key1, xid));
        this.accountDao.updateLock(Pair.asPair(key2, xid));
        return true;
    }

    synchronized public void releaseLock(String key1, String key2, String xid) {
        if (!this.containBoth(key1, key2)) {
            throw new ShouldNotHappenException();
        }
        keyMap.remove(key1);
        keyMap.remove(key2);
        accountDao.updateLock(Pair.asPair(key1, null));
        accountDao.updateLock(Pair.asPair(key2, null));
    }

    public boolean areBothLockedCorrectlly(String key1, String key2, String xid) {
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

    synchronized public void releaseByXid(String xid) {
        final List<String> keys = new ArrayList<String>(2);
        for (String k : keyMap.keySet()) {
            if (Objects.equal(xid, keyMap.get(k))) {
                keys.add(k);
            }
        }
        if (keys.size() == 0) {
            return;
        } else if (keys.size() != 2) {
            throw new ShouldNotHappenException("releaseByXid num of locked key is " + keys.size());
        } else {
            for (String k : keys) {
                keyMap.remove(k);
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