package sos.accumulo.monitor.tracker;

public interface AnnounceDao {
    
    public String getAnnounceAddress();

    public void announceRunner();
}