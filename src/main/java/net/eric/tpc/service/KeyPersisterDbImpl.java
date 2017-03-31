package net.eric.tpc.service;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import net.eric.tpc.base.Pair;
import static net.eric.tpc.base.Pair.asPair;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.persist.KeyRecord;
import net.eric.tpc.persist.KeyStoreDao;

public class KeyPersisterDbImpl implements KeyGenerator.KeyPersister{
    
    private KeyStoreDao keyStoreDao;

    @Override
    public Map<String, Pair<Integer, Integer>> loadStoredKeys() {
        List<KeyRecord> keys = keyStoreDao.selectAll();
        Map<String, Pair<Integer, Integer>> keyMap = Maps.newHashMap();
        for(KeyRecord key : keys){
            keyMap.put(key.getPrefix(), asPair(key.getDateDigit(), key.getSerialNumber()));
        }
        return keyMap;
    }

    @Override
    public void storeKey(String keyName, int dateDigit, int serial) {
        KeyRecord key = keyStoreDao.selectByPrefix(keyName);
        if(key == null){
            keyStoreDao.insert(new KeyRecord(keyName, dateDigit, serial));
        }else{
            key.setDateDigit(dateDigit);
            key.setSerialNumber(serial);
            keyStoreDao.update(key);
        }
    }

    public KeyStoreDao getKeyStoreDao() {
        return keyStoreDao;
    }

    public void setKeyStoreDao(KeyStoreDao keyStoreDao) {
        this.keyStoreDao = keyStoreDao;
    }
    
    
}
