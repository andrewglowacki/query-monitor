package sos.accumulo.monitor.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnouncementThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementThread.class);
    private final String name;
    private final AnnounceDao monitorDao;
    private final String trackerAddress;

    public AnnouncementThread(AnnounceDao monitorDao, String name, String trackerAddress) {
        this.monitorDao = monitorDao;
        this.name = name;
        this.trackerAddress = trackerAddress;
    }

    @Override
    public void run() {
        try {
            monitorDao.announceRunner(name, trackerAddress);
        } catch (Throwable ex) {
            log.error("Failed to announce presence to " + monitorDao.getAnnounceAddress(), ex);
        }
    }

}