package net.eric.tpc.persist;

import java.util.List;

public interface KeyStoreDao {
    List<KeyRecord> selectAll();

    KeyRecord selectByPrefix(String prefix);

    void insert(KeyRecord keyRecord);

    void update(KeyRecord keyRecord);
}
