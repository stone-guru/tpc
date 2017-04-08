package net.eric.tpc.service;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import net.eric.tpc.base.Pair;
import static net.eric.tpc.base.Pair.asPair;

import net.eric.tpc.persist.KeyRecord;
import net.eric.tpc.persist.KeyStoreDao;

public class KeyPersisterDbImpl implements KeyGenerators.KeyPersister{
    
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
        //FIXME 这样效率不高，将就先
        KeyRecord storedKey = keyStoreDao.selectByPrefix(keyName);
        if(storedKey == null){
            keyStoreDao.insert(new KeyRecord(keyName, dateDigit, serial));
        }else{
            if(storedKey.getDateDigit() > dateDigit || storedKey.getSerialNumber() > serial){
                return;
            }
            storedKey.setDateDigit(dateDigit);
            storedKey.setSerialNumber(serial);
            keyStoreDao.update(storedKey);
        }
    }

    public KeyStoreDao getKeyStoreDao() {
        return keyStoreDao;
    }

    public void setKeyStoreDao(KeyStoreDao keyStoreDao) {
        this.keyStoreDao = keyStoreDao;
    }
    
    
}
