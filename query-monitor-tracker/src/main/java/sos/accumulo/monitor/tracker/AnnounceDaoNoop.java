package sos.accumulo.monitor.tracker;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile({ "TrackerModeProxy", "TrackerModeExecutor" })
@Repository
public class AnnounceDaoNoop implements AnnounceDao {

    @Override
    public String getAnnounceAddress() {
        return null;
    }

    @Override
    public void announceRunner() {
        // NOOP
    }
    
}