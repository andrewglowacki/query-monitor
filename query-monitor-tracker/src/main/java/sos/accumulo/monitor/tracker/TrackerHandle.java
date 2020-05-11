package sos.accumulo.monitor.tracker;

import java.io.Closeable;

public class TrackerHandle implements Closeable {

    private final String trackerAddress;
    private boolean closed = false;

    public TrackerHandle(String trackerAddress) {
        this.trackerAddress = trackerAddress;
        TrackerServer.newTrackerHandle();
    }

    public String getTrackerAddress() {
        return trackerAddress;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        TrackerServer.close();
        closed = true;
    }

}