package net.eric.tpc.net.binary;

import org.apache.mina.core.buffer.IoBuffer;

public interface ObjectCodec {
    short getTypeCode();

    Class<?> getObjectClass();

    void encode(Object entity, IoBuffer buf) throws Exception;

    Object decode(int length, IoBuffer in) throws Exception;
}
