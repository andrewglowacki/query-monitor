package sos.accumulo.monitor.tracker;

import java.io.IOException;

public interface MonitorDao {
    
    public String getAnnounceAddress();

    public void announceRunner(String name, String trackerAddress) throws IOException;
}