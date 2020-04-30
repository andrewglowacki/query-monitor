package sos.accumulo.monitor.tracker;

import java.io.Closeable;
import java.io.IOException;

public class TrackerHandle implements Closeable {

    private boolean closed = false;

    public TrackerHandle() { 
        TrackerServer.newTrackerHandle();
    }

    public String getTrackerAddress() {
        return TrackerServer.getTrackerAddress();
    }

    public void waitForTrackerServer() {
        while (TrackerServer.getTrackerAddress() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
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