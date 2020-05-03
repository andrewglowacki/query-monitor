package sos.accumulo.monitor.tracker;

public class RegisteredProxy {

    private final long started = System.currentTimeMillis();
    private final long queryIndex;
    private final String address;
    private final String id;

    public RegisteredProxy(long queryIndex, String address, String id) {
        this.queryIndex = queryIndex;
        this.address = address;
        this.id = id;
    }

    public long getStarted() {
        return started;
    }

    public long getQueryIndex() {
        return queryIndex;
    }

    public String getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

}