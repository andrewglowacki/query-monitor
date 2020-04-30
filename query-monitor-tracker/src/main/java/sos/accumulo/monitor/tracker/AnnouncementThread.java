package sos.accumulo.monitor.tracker;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sos.accumulo.monitor.util.HttpQuery;

public class AnnouncementThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementThread.class);
    private final String name;
    private final String announceAddress;
    private final String trackerAddress;

    public AnnouncementThread(String announceAddress, String name, String trackerAddress) {
        this.name = name;
        this.announceAddress = announceAddress;
        this.trackerAddress = trackerAddress;
    }

    @Override
    public void run() {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicHeader("name", name));
            params.add(new BasicHeader("address", trackerAddress));
            HttpQuery.normalPostQuery("http://" + announceAddress + "/api/runner/register", params);
        } catch (Throwable ex) {
            log.error("Failed to announce presence to " + announceAddress, ex);
        }
    }

}