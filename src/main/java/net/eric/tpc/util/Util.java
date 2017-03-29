package net.eric.tpc.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import net.eric.tpc.common.ShouldNotHappenException;

public class Util {
    public static byte[] ObjectToBytes(Object obj) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            throw new ShouldNotHappenException(e);
        }

        return bytes;
    }
}
