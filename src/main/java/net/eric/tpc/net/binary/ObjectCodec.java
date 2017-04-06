package net.eric.tpc.net.binary;

public interface ObjectCodec {
    short getTypeCode();
    Class<?> getObjectClass();
    byte[] encode(Object entity) throws Exception;
    Object decode(byte[] bytes) throws Exception;
}
