package sos.accumulo.monitor.tracker;

import java.io.Closeable;
import java.io.IOException;

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
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        TrackerServer.close();
        closed = true;
    }

}