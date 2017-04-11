package net.eric.tpc.net.binary;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
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
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.Types.TransStartRec;

public class MessageCodecFactory implements ProtocolCodecFactory {

    private static CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
    private static CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();

    private MessageEncoder encoder = new MessageEncoder();
    private MessageDecoder decoder = new MessageDecoder();

    private Map<Short, ObjectCodec> objectCodecTypeCodeMap = Collections.emptyMap();
    private Map<Class<?>, ObjectCodec> objectCodecClassMap = Collections.emptyMap();

    public MessageCodecFactory() {
        this(Collections.emptyList());
    }

    public MessageCodecFactory(List<ObjectCodec> objectCodecs) {
        ImmutableList.Builder<ObjectCodec> listBuilder = ImmutableList.builder();
        //listBuilder.add(new SerializeCodec(ActionStatus.class, Message.TypeCode.ACTION_STATUS));
        //listBuilder.add(new SerializeCodec(TransStartRec.class, Message.TypeCode.TRANS_START_REC));
        // listBuilder.add(new SerializeCodec(TransferBill.class,
        // Message.TypeCode.TRANSFER_BILL));
        listBuilder.add(new ActionStatusCodec());
        listBuilder.add(new TransStartRecCodec());
        //listBuilder.add(new TransferBillCodec());

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

    private ObjectCodec getObjectCodecForTypeCode(short code) {
        ObjectCodec c = this.objectCodecTypeCodeMap.get(code);
        if (c == null) {
            throw new IllegalArgumentException("ObjectCodec for typecode not defined " + code);
        }
        return c;
    }

    private ObjectCodec getObjectCodecForClass(Class<?> clz) {
        ObjectCodec c = this.objectCodecClassMap.get(clz);
        if (c == null) {
            throw new IllegalArgumentException("ObjectCodec for class not defined " + clz.getCanonicalName());
        }
        return c;
    }

    class MessageEncoder implements ProtocolEncoder {
        final private Pair<Short, Integer> NO_OBJECT = Pair.asPair((short) 0, 0);

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            Message msg = (Message) message;
            IoBuffer buf = IoBuffer.allocate(32).setAutoExpand(true);

            Pair<Short, Integer> param = encodeObject(msg.getParam(), buf, 32);
            Pair<Short, Integer> content = encodeObject(msg.getContent(), buf, 32 + param.snd());
            final int length = param.snd() + content.snd() + 28;
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
            // total length is data length plus length field int (4 bytes)
            buf.position(length + 4);
            buf.flip();
            out.write(buf);
        }

        @Override
        public void dispose(IoSession session) throws Exception {
        }

        Pair<Short, Integer> encodeObject(Object entity, IoBuffer buf, int pos) throws Exception {
            if (entity == null) {
                return NO_OBJECT;
            }
            ObjectCodec codec = MessageCodecFactory.this.getObjectCodecForClass(entity.getClass());
            buf.position(pos);
            codec.encode(entity, buf);
            int length = buf.position() - pos;
            return Pair.asPair(codec.getTypeCode(), length);
        }
    }

    class MessageDecoder extends CumulativeProtocolDecoder {

        @Override
        public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (!in.prefixedDataAvailable(4, 1024 * 1024)) {
                return false;
            }

            // int n = in.getInt(0);
            // byte[] bytes = new byte[n + 4];
            // in.get(bytes);
            // FileOutputStream fs = new FileOutputStream("/tmp/message.bin");
            // fs.write(bytes);
            // fs.close();
            // if(n > 0)
            // throw new RuntimeException();

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
            
            msg.setSender(session.getRemoteAddress());
            
            out.write(msg);

            return true;
        }

        private Object decodeObject(short typeCode, int length, IoBuffer in) throws Exception {
            if (typeCode == 0) {
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

        @Override
        public short getTypeCode() {
            return Message.TypeCode.ACTION_STATUS;
        }

        @Override
        public Class<?> getObjectClass() {
            return ActionStatus.class;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            ActionStatus status = (ActionStatus) entity;
            buf.putShort(status.getCode());
            buf.putString(status.getDescription(), MessageCodecFactory.utf8Encoder);
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            short code = in.getShort();
            String s = in.getString(length - 2, utf8Decoder);
            return new ActionStatus(code, s);
        }
    }

    static class InetSocketAdressCodec extends SizeAheadObjectCodec {

        @Override
        public short getTypeCode() {
            throw new ShouldNotHappenException();
        }

        @Override
        public Class<?> getObjectClass() {
            throw new ShouldNotHappenException();
        }

        @Override
        public void doEncode(Object entity, IoBuffer buf) throws Exception {
            InetSocketAddress address = (InetSocketAddress) entity;
            buf.putInt(address.getPort());
            if (address.isUnresolved()) {
                buf.put((byte) 0);
                buf.putString(address.getHostString(), MessageCodecFactory.utf8Encoder);
                buf.put((byte) 0);
            } else {
                buf.put((byte) 1);
                byte[] ip = address.getAddress().getAddress();
                buf.put(ip);
            }
        }

        @Override
        public Object doDecode(int length, IoBuffer in) throws Exception {
            int port = in.getInt();
            byte tag = in.get();
            if (tag == 0) {
                String host = in.getString(MessageCodecFactory.utf8Decoder);
                return InetSocketAddress.createUnresolved(host, port);
            } else if (tag == 1) {
                byte[] ip = new byte[length - 5];
                in.get(ip);
                return new InetSocketAddress(InetAddress.getByAddress(ip), port);
            } else {
                throw new IllegalArgumentException("InetSocketAddress tag not 0, 1" + tag);
            }
        }
    }

    static class TransStartRecCodec implements ObjectCodec {
        private InetSocketAdressCodec addressCodec = new InetSocketAdressCodec();

        @Override
        public short getTypeCode() {
            return Message.TypeCode.TRANS_START_REC;
        }

        @Override
        public Class<?> getObjectClass() {
            return TransStartRec.class;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            TransStartRec rec = (TransStartRec) entity;
            buf.putLong(rec.getXid());
            addressCodec.encode(rec.getCoordinator(), buf);
            buf.putInt(rec.getParticipants().size());
            for (InetSocketAddress addr : rec.getParticipants()) {
                addressCodec.encode(addr, buf);
            }
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            long xid = in.getLong();
            InetSocketAddress coorAddress = (InetSocketAddress) addressCodec.decode(0, in);

            int n = in.getInt();
            List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>(n);
            for (int i = 0; i < n; i++) {
                addresses.add((InetSocketAddress) addressCodec.decode(0, in));
            }

            return new TransStartRec(xid, coorAddress, addresses);
        }
    }
}
