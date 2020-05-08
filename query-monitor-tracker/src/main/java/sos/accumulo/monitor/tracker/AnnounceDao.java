package sos.accumulo.monitor.tracker;

import java.io.IOException;

public interface AnnounceDao {
    
    public String getAnnounceAddress();

    public void announceRunner(String name, String trackerAddress) throws IOException;
}