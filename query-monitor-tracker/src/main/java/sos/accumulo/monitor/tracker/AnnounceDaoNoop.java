package sos.accumulo.monitor.tracker;

import java.io.IOException;

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
    public void announceRunner(String name, String trackerAddress) throws IOException {
        // NOOP
    }
    
}