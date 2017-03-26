package net.eric.tpc.trans.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.eric.tpc.common.KeyGenerator;

public class KeyGeneratorTest {
    public static class KeyPrintTask implements Runnable {
        private String prefix;

        public KeyPrintTask(String prefix) {
            this.prefix = prefix;
        }

        public void run() {
            try {
                final String k1 = KeyGenerator.nextKey(this.prefix);
                System.out.println(k1);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        KeyGenerator.init();
        for (int i = 0; i < 100; i++) {
            pool.submit(new KeyPrintTask("TRANS"));
            pool.submit(new KeyPrintTask("PACKET"));
        }
        pool.shutdown();
    }
}
