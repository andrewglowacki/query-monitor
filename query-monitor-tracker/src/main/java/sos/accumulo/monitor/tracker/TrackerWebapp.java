package sos.accumulo.monitor.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TrackerWebapp
{
    private static final Logger log = LoggerFactory.getLogger(TrackerWebapp.class);
    private static int activeCount = 0;
    private static ConfigurableApplicationContext context;
    private static TrackerMode mode;

    public static synchronized TrackerHandle run(TrackerMode mode) {
        if (context == null) {
            if (activeCount != 0) {
                log.warn("Tracker webapp active count is non-zero, however no context exists.");
            }
            log.info("Creating tracker webapp");
            System.setProperty("spring.profiles.active", "TrackerMode" + mode);
            context = SpringApplication.run(TrackerWebapp.class);
            activeCount = 1;
            TrackerWebapp.mode = mode;
        } else if (TrackerWebapp.mode != mode) {
            throw new IllegalArgumentException("Tracker webapp is already running in " + TrackerWebapp.mode + " mode");
        } else {
            activeCount++;
        }
		return new TrackerHandle();
    }

    protected static synchronized void close() {
        activeCount--;
        if (activeCount <= 0) {
            if (activeCount < 0) {
                log.warn("Active tracker count is less than zero");
                activeCount = 0;
            }
            if (context != null) {
                log.info("Stopping tracker webapp - no more active clients");
                context.close();
                context = null;
            }
        }
    }
}
