package sos.accumulo.monitor.tracker;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import sos.accumulo.monitor.util.HttpQuery;

@Profile("TrackerModeRunner")
@Repository
public class AnnounceDaoImpl implements AnnounceDao {

    private static final Logger log = LoggerFactory.getLogger(AnnounceDaoImpl.class);

    @Value("${announce.address}")
    private String announceAddress;

    @Value("${runner.name}")
    private String runnerName;

    @Override
    public String getAnnounceAddress() {
        return announceAddress;
    }

    @Autowired
    private TrackerAddress address;

    @Override
    public void announceRunner() {
        try {
            if (announceAddress.equals("test-mode")) {
                return;
            }
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicHeader("name", runnerName));
            params.add(new BasicHeader("address", address.get()));
            HttpQuery.normalPostQuery("http://" + announceAddress + "/api/runner/register", params);
        } catch (Throwable ex) {
            log.error("Failed to announce presence to " + announceAddress, ex);
        }
    }
}