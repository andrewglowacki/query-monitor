package sos.accumulo.monitor.data;

public class AccumuloScanInfo {
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

    
}