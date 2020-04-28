package sos.accumulo.monitor.tracker;

import java.io.Closeable;
import java.io.IOException;

public class TrackerHandle implements Closeable {

    private boolean closed = false;
    public TrackerHandle() { }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        TrackerWebapp.close();
        closed = true;
    }

}