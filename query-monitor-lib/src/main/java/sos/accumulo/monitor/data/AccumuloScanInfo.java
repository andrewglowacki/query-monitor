package sos.accumulo.monitor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccumuloScanInfo {
    private static final Pattern BATCH_SCANNER_PATTERN = Pattern.compile(".*looking up ([0-9]+) ranges at (.+)");
    private static final Pattern SCANNER_PATTERN = Pattern.compile("(Starting|Continuing) scan tserver=(.+) (?:scanid|tableId)=(.+)");
    private String server;
    private int ranges;
    private String table;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getRanges() {
        return ranges;
    }

    public void setRanges(int ranges) {
        this.ranges = ranges;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public static List<AccumuloScanInfo> listScans() {
        List<AccumuloScanInfo> scans = new ArrayList<>();
        
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }

        Thread[] threads = new Thread[root.activeCount() + 10];
        int count = 0;
        while ((count = root.enumerate(threads, true)) == threads.length) {
            threads = new Thread[count * 2];
        }

        for (int i = 0; i < count; i++) {
            Thread thread = threads[i];
            String name = thread.getName();
            Matcher scanMatcher = SCANNER_PATTERN.matcher(name);
            if (scanMatcher.matches()) {
                String tserver = scanMatcher.group(2);
                String tableId = null;
                if (scanMatcher.group(1).equals("Starting")) {
                    tableId = scanMatcher.group(3);
                }
                AccumuloScanInfo scan = new AccumuloScanInfo();
                scan.setTable(tableId);
                scan.setServer(tserver);
                scan.setRanges(1);
                scans.add(scan);
                continue;
            }

            Matcher batchMatcher = BATCH_SCANNER_PATTERN.matcher(name);
            if (batchMatcher.matches()) {
                int ranges = 0;
                try {
                    ranges = Integer.parseInt(batchMatcher.group(1));
                } catch (NumberFormatException ex) { }

                AccumuloScanInfo scan = new AccumuloScanInfo();
                scan.setServer(batchMatcher.group(2));
                scan.setRanges(ranges);
                scans.add(scan);
            }
        }

        return scans;
    }

    @Override
    public String toString() {
        return "{ranges=" + ranges + ", server=" + server + ", table=" + table + "}";
    }

    
}