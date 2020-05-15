package sos.accumulo.monitor.tests;

public class FakeScan {
    public static void run() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ex) { }
            }
        });
        thread.setName("Starting scan tserver=r1n02-node tableId=a");
        thread.setDaemon(true);
        thread.start();
    }

    public static void run(int count) {
        for (int i = 0; i < count; i++) {
            run();
        }
    }
}