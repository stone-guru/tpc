package net.eric.tpc.net.binary;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
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
import net.eric.tpc.base.ActionStatus2;
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
        listBuilder.add(new ActionStatusCodec());
        
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


    class MessageEncoder implements ProtocolEncoder {
        final private Pair<Short, Integer> NO_OBJECT = Pair.asPair((short)0, 0);
        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            Message msg = (Message) message;
            IoBuffer buf = IoBuffer.allocate(32).setAutoExpand(true);
            
            Pair<Short, Integer> param = encodeObject(msg.getParam(), buf, 32);
            Pair<Short, Integer>  content = encodeObject(msg.getContent(), buf, 32 + param.snd());
            final int length = param.snd() + content.snd() + 28;
            System.out.println("length = " + length);
            buf.position(0);
            buf.putInt(length);
            buf.putShort(msg.getVersion());
            buf.putShort(msg.getRound());
            buf.putLong(msg.getXid());
            buf.putShort(msg.getCommandCode());
            buf.putShort(msg.getCommandAnswer());
            buf.putShort(param.fst());
            buf.putShort(content.fst());
            buf.putInt(param.snd());
            buf.putInt(content.snd());

            System.out.println("buf capacity " + buf.capacity());
            buf.position(length + 4);//total length is data length plus length field int (4 bytes)
            buf.flip();
            out.write(buf);
        }

        @Override
        public void dispose(IoSession session) throws Exception {
        }
        
        Pair<Short, Integer> encodeObject(Object entity, IoBuffer buf, int pos) throws Exception {
            if(entity == null){
                return NO_OBJECT;
            }
            ObjectCodec codec = MessageCodecFactory.this.getObjectCodecForClass(entity.getClass());
            //buf.expand(pos, 128);
            buf.position(pos);
            codec.encode(entity, buf);
            int length = buf.position() - pos;
            System.out.println("encodeObject position is " + buf.position());
            return Pair.asPair(codec.getTypeCode(), length);
        }
    }

    class MessageDecoder extends CumulativeProtocolDecoder {

        @Override
        public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            System.out.println("get length " + in.getInt(0));
            if (!in.prefixedDataAvailable(4, 1024 * 1024)) {
                System.out.println("wait more");
                System.out.println("remain is " + in.remaining());
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
            msg.setParam(this.decodeObject(paramType, paramLength, in));
            msg.setContent(this.decodeObject(contentType, contentLength, in));
            
            out.write(msg);

            return true;
        }
        
        private Object decodeObject(short typeCode, int length, IoBuffer in) throws Exception {
            if(typeCode == 0){
                return null;
            }
            ObjectCodec c = MessageCodecFactory.this.getObjectCodecForTypeCode(typeCode);
            return c.decode(length, in);
        }
    }
    
    
    static class SerializeCodec implements ObjectCodec {
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
        public Class<?> getObjectClass() {
            return this.objectClass;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            buf.putObject(entity);
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            return in.getObject();
        }
    }
    
    static class ActionStatusCodec implements ObjectCodec {
        private CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
        private CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
        
        @Override
        public short getTypeCode() {
            return Message.TypeCode.ACTION_STATUS2;
        }

        @Override
        public Class<?> getObjectClass() {
            return ActionStatus2.class;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            ActionStatus2 status = (ActionStatus2)entity;
            buf.putShort(status.getCode());
            buf.putString(status.getDescription(), utf8Encoder);
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            short code = in.getShort();
            String s = in.getString(length - 2, utf8Decoder);
            return new ActionStatus2(code, s);
        }
    }
    
   static class NodeCodec implements ObjectCodec {

    @Override
    public short getTypeCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Class<?> getObjectClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encode(Object entity, IoBuffer buf) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Object decode(int length, IoBuffer in) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
       
   }
   
   static class TransStartRecCodec implements ObjectCodec {

        @Override
        public short getTypeCode() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Class<?> getObjectClass() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
