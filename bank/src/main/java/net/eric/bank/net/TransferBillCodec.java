package net.eric.bank.net;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Date;

import org.apache.mina.core.buffer.IoBuffer;

import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.tpc.net.binary.ObjectCodec;

public class TransferBillCodec implements ObjectCodec{
    public static final short BILL_TYPE_CODE = 2001;
    

    private static CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
    private static CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
    
    @Override
    public short getTypeCode() {
        return BILL_TYPE_CODE;
    }

    @Override
    public Class<?> getObjectClass() {
        return TransferBill.class;
    }

    @Override
    public void encode(Object entity, IoBuffer buf) throws Exception {
        TransferBill bill = (TransferBill)entity;
        buf.putString(bill.getTransSN(), utf8Encoder).put((byte)0);
        buf.putLong(bill.getAmount().scaleByPowerOfTen(2).longValue());
        buf.putLong(bill.getLaunchTime().getTime());
        buf.putString(bill.getReceivingBankCode(), utf8Encoder).put((byte)0);
        buf.putString(bill.getPayer().getNumber(), utf8Encoder).put((byte)0);
        buf.putString(bill.getPayer().getBankCode(), utf8Encoder).put((byte)0);
        buf.putString(bill.getReceiver().getNumber(), utf8Encoder).put((byte)0);
        buf.putString(bill.getReceiver().getBankCode(), utf8Encoder).put((byte)0);
        buf.putString(bill.getVoucherNumber(), utf8Encoder).put((byte)0);
        buf.putString(bill.getSummary(), utf8Encoder).put((byte)0);
    }

    @Override
    public Object decode(int length, IoBuffer in) throws Exception {
        TransferBill bill = new TransferBill();
        bill.setTransSN(in.getString(utf8Decoder));
        bill.setAmount(new BigDecimal(in.getLong()).scaleByPowerOfTen(-2));
        bill.setLaunchTime(new Date(in.getLong()));
        bill.setReceivingBankCode(in.getString(utf8Decoder));
        String number1 = in.getString(utf8Decoder);
        String bankCode1 = in.getString(utf8Decoder);
        bill.setPayer(new AccountIdentity(number1, bankCode1));
        String number2 = in.getString(utf8Decoder);
        String bankCode2 = in.getString(utf8Decoder);
        bill.setReceiver(new AccountIdentity(number2, bankCode2));
        bill.setVoucherNumber(in.getString(utf8Decoder));
        bill.setSummary(in.getString(utf8Decoder));
        return bill;
    }
}
