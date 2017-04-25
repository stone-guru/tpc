package net.eric.tpc.proto;

import org.apache.mina.core.buffer.IoBuffer;

import net.eric.tpc.net.binary.ObjectCodec;

public class IntCodec implements ObjectCodec {

    @Override
    public short getTypeCode() {
        return 2001;
    }

    @Override
    public Class<?> getObjectClass() {
        return Integer.class;
    }

    @Override
    public void encode(Object entity, IoBuffer buf) throws Exception {
        buf.putInt((Integer) entity);
    }

    @Override
    public Object decode(int length, IoBuffer in) throws Exception {
        return in.getInt();
    }
}