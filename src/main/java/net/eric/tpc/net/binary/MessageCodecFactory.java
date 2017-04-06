package net.eric.tpc.net.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.Types.TransStartRec;

public class MessageCodecFactory implements ProtocolCodecFactory {

    private MessageEncoder encoder = new MessageEncoder();

    private MessageDecoder decoder = new MessageDecoder();

    private Map<Short, ObjectCodec> objectCodecTypeCodeMap = Collections.emptyMap();
    private Map<Class<?>, ObjectCodec> objectCodecClassMap = Collections.emptyMap();

    public MessageCodecFactory() {
        this(Collections.emptyList());
    }

    public MessageCodecFactory(List<ObjectCodec> objectCodecs) {
        ImmutableList.Builder<ObjectCodec> listBuilder = ImmutableList.builder();
        listBuilder.add(new SerializeCodec(ActionStatus.class, Message.TypeCode.ACTION_STATUS));
        listBuilder.add(new SerializeCodec(TransStartRec.class, Message.TypeCode.TRANS_START_REC));
        listBuilder.add(new SerializeCodec(TransferBill.class, Message.TypeCode.TRANSFER_BILL));

        Builder<Short, ObjectCodec> codeMapBuilder = ImmutableMap.builder();
        Builder<Class<?>, ObjectCodec> classMapBuilder = ImmutableMap.builder();

        for (ObjectCodec c : listBuilder.build()) {
            codeMapBuilder.put(c.getTypeCode(), c);
            classMapBuilder.put(c.getObjectClass(), c);
        }

        this.objectCodecTypeCodeMap = codeMapBuilder.build();
        this.objectCodecClassMap = classMapBuilder.build();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return this.encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return this.decoder;
    }

    private ObjectCodec getObjectCodecForTypeCode(short code){
        ObjectCodec c = this.objectCodecTypeCodeMap.get(code);
        if(c == null){
            throw new IllegalArgumentException("ObjectCodec for typecode not defined " + code);
        }
        return c;
    }
    
    private ObjectCodec getObjectCodecForClass(Class<?> clz){
        ObjectCodec c = this.objectCodecClassMap.get(clz);
        if(c == null){
            throw new IllegalArgumentException("ObjectCodec for class not defined " + clz.getCanonicalName());
        }
        return c;
    }
    
    class SerializeCodec implements ObjectCodec {
        private short typeCode = 0;
        private Class<?> objectClass;

        public SerializeCodec(Class<?> objectClass, short typeCode) {
            this.typeCode = typeCode;
            this.objectClass = objectClass;
        }

        @Override
        public short getTypeCode() {
            return this.typeCode;
        }

        @Override
        public byte[] encode(Object entity) throws Exception {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bo);
            oos.writeObject(entity);
            return bo.toByteArray();
        }

        @Override
        public Object decode(byte[] bytes) throws Exception {
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return is.readObject();
        }

        @Override
        public Class<?> getObjectClass() {
            return this.objectClass;
        }
    }

    class MessageEncoder implements ProtocolEncoder {
        final private Pair<Short, byte[]> NO_OBJECT = Pair.asPair((short)0, new byte[0]);
        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            Message msg = (Message) message;
            Pair<Short, byte[]> paramBytes = encodeObject(msg.getParam());
            Pair<Short, byte[]> contentBytes = encodeObject(msg.getContent());
            final int length = paramBytes.fst() + contentBytes.fst() + 28;
            
            IoBuffer buf = IoBuffer.allocate(length + 4).setAutoExpand(true);
            buf.putInt(length);
            buf.putShort(msg.getVersion());
            buf.putShort(msg.getRound());
            buf.putLong(msg.getXid());
            buf.putShort(msg.getCommandCode());
            buf.putShort(msg.getCommandAnswer());
            buf.putShort(paramBytes.fst());
            buf.putShort(contentBytes.fst());
            buf.putInt(paramBytes.snd().length);
            buf.putInt(contentBytes.snd().length);
            buf.put(paramBytes.snd());
            buf.put(contentBytes.snd());

            buf.flip();
            out.write(buf);
        }

        @Override
        public void dispose(IoSession session) throws Exception {
        }
        
        Pair<Short, byte[]> encodeObject(Object entity) throws Exception {
            if(entity == null){
                return NO_OBJECT;
            }
            ObjectCodec codec = MessageCodecFactory.this.getObjectCodecForClass(entity.getClass());
            byte[] bytes = codec.encode(entity);
            return Pair.asPair(codec.getTypeCode(), bytes);
        }
    }

    class MessageDecoder extends CumulativeProtocolDecoder {

        @Override
        public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (!in.prefixedDataAvailable(4, 1024 * 1024)) {
                return false;
            }
            Message msg = new Message();
            @SuppressWarnings("unused")
            final int length = in.getInt();
            msg.setVersion(in.getShort());
            msg.setRound(in.getShort());
            msg.setXid(in.getLong());
            msg.setCommandCode(in.getShort());
            msg.setCommandAnswer(in.getShort());
            short paramType = in.getShort();
            short contentType = in.getShort();
            int paramLength = in.getInt();
            int contentLength = in.getInt();

            byte[] paramBytes = new byte[paramLength];
            in.get(paramBytes);
            msg.setParam(this.decodeObject(paramType, paramBytes));

            byte[] contentBytes = new byte[contentLength];
            in.get(contentBytes);
            msg.setContent(this.decodeObject(contentType, contentBytes));
            
            out.write(msg);

            return true;
        }
        
        private Object decodeObject(short typeCode, byte[] bytes) throws Exception {
            if(typeCode == 0){
                return null;
            }
            ObjectCodec c = MessageCodecFactory.this.getObjectCodecForTypeCode(typeCode);
            return c.decode(bytes);
        }
    }

}
