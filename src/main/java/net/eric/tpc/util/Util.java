package net.eric.tpc.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.base.Preconditions;

import net.eric.tpc.bank.AccountRepositoryImpl;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.entity.Account;

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

    public static void displayAccounts(List<Account> accounts) {
        int[] lengthes = new int[] { 10, 14, 12, 15, 8, 6};
        String title = Util.prettyFormat(lengthes,
                new Object[] { "AcctNumber", "AcctName", "Balance", "Overdraft Limit", "Type", "Bank" });
        System.out.println("Current accounts");
        System.out.println(title);
        for (Account acct : accounts) {
            Object[] values = new Object[] { acct.getAcctNumber(), acct.getAcctName(), acct.getBalance(),
                    acct.getOverdraftLimit(), acct.getType(), acct.getBankCode() };
            String s = Util.prettyFormat(lengthes, values);
            System.out.println(s);
        }
    }

    public static String prettyFormat(int[] lengthes, Object[] values) {

        Preconditions.checkNotNull(lengthes);
        Preconditions.checkNotNull(values);
        Preconditions.checkArgument(lengthes.length == values.length, "length of these two array not equal");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String s;
            if (values[i] instanceof Date) {
                synchronized (formater) {
                    s = formater.format((Date) values[i]);
                }
            } else if (values[i] instanceof BigDecimal) {
                s = "$" + values[i].toString();
            } else {
                s = String.valueOf(values[i]);
            }
            if (i > 0) {
                sb.append(", ");
            }
            appendString(sb, s, lengthes[i]);
        }
        return sb.toString();
    }

    private static SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static void appendString(StringBuilder sb, String s, int length) {
        if (s.length() > length) {
            sb.append(s.substring(0, length));
        } else {
            sb.append(s);
            for (int i = 0; i < length - s.length(); i++) {
                sb.append(' ');
            }
        }
    }
}
