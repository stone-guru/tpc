package net.eric.tpc.net.binary;

import org.apache.mina.core.buffer.IoBuffer;

public abstract class SizeAheadObjectCodec implements ObjectCodec {
    @Override
    final public void encode(Object entity, IoBuffer buf) throws Exception {
        int startPos = buf.position();
        buf.putInt(0);
        
        this.doEncode(entity, buf);
        int endPos = buf.position();

        buf.position(startPos);
        buf.putInt(endPos - startPos - 4);
        buf.position(endPos);
    }

    @Override
    final public Object decode(int length, IoBuffer in) throws Exception {
        int realLength = in.getInt();
        return this.doDecode(realLength, in);
    }

    abstract protected void doEncode(Object entity, IoBuffer buf) throws Exception;

    abstract protected Object doDecode(int length, IoBuffer buf) throws Exception;
}
