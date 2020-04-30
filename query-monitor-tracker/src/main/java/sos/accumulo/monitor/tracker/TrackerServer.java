package sos.accumulo.monitor.tracker;

import java.net.Inet4Address;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TrackerServer
{
    private static final Logger log = LoggerFactory.getLogger(TrackerServer.class);
    private static int activeCount = 0;
    private static ConfigurableApplicationContext context;
    private static TrackerMode mode;
    private static volatile String trackerAddress = null;
    private volatile ScheduledExecutorService announceThread = null;

    @LocalServerPort
    private int port;

    @Value("${announce.address}")
    private String announceAddress;

    @Value("${tracker.host:}")
    private String trackerHost;

    @Value("${runner.name:}")
    private String runnerName;

    @PostConstruct
    public void setup() {
        if (System.getProperty("spring.profiles.active", "").equals("TrackerMode" + TrackerMode.Proxy)) {
            return;
        }

        if (trackerHost != null && !trackerHost.isEmpty()) {
            trackerAddress = trackerHost + ":" + port;
        } else {
            try {
                trackerAddress = Inet4Address.getLocalHost().getHostAddress() + ":" + port;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        announceThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("Query Monitor Tracker Announcement Thread - To: " + announceAddress);
                return thread;
            }
        });
        announceThread.scheduleWithFixedDelay(new AnnouncementThread(announceAddress, runnerName, trackerAddress), 0, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void cleanup() {
        if (announceThread != null) {
            announceThread.shutdownNow();
        }
        trackerAddress = null;
    }

    protected static String getTrackerAddress() {
        return trackerAddress;
    }

    public static synchronized TrackerHandle startProxy() {
        if (context != null) {
            if (TrackerServer.mode != TrackerMode.Proxy) {
                throw new IllegalArgumentException("Tracker server is already running in Proxy mode");
            }
            return new TrackerHandle();
        }
        System.setProperty("server.address", "" + Inet4Address.getLoopbackAddress().getHostAddress());
		return startServer(TrackerMode.Proxy);
    }

    public static synchronized TrackerHandle startExecutorServer() {
        if (context != null) {
            if (TrackerServer.mode != TrackerMode.Executor) {
                throw new IllegalArgumentException("Tracker server is already running in " + mode + " mode");
            }
            return new TrackerHandle();
        }
        return startServer(TrackerMode.Executor);
    }
    public static synchronized TrackerHandle startQueryRunnerServer(String announceAddress, String name) {
        if (context != null) {
            if (TrackerServer.mode != TrackerMode.Runner) {
                throw new IllegalArgumentException("Tracker server is already running in " + mode + " mode");
            }
            return new TrackerHandle();
        }
        
        if (announceAddress == null || announceAddress.isEmpty()) {
            if (System.getProperty("announce.address", "").isEmpty()) {
                throw new IllegalArgumentException("Announce address is not set but must be for 'Runner' mode tracker.");
            }
            announceAddress = System.getProperty("announce.address");
        } else {
            System.setProperty("announce.address", announceAddress);
        }

        if (name == null || name.isEmpty()) {
            if (System.getProperty("runner.name", "").isEmpty()) {
                throw new IllegalArgumentException("Runner name is not set but must be for 'Runner' mode tracker.");
            }
            name = System.getProperty("runner.name");
        } else {
            System.setProperty("runner.name", name);
        }

        return startServer(TrackerMode.Runner);
    }
    
    protected static synchronized TrackerHandle startServer(TrackerMode mode) {
        if (activeCount != 0) {
            log.warn("Tracker server active count is non-zero, however no context exists.");
        }
        log.info("Creating tracker server with mode: " + mode);
        System.setProperty("spring.profiles.active", "TrackerMode" + mode);
        System.setProperty("server.port", "0");
        context = SpringApplication.run(TrackerServer.class);
        activeCount = 0;
        TrackerServer.mode = mode;
		return new TrackerHandle();
    }

    protected static synchronized void newTrackerHandle() {
        activeCount++;
    }
    protected static synchronized void close() {
        activeCount--;
        if (activeCount <= 0) {
            if (activeCount < 0) {
                log.warn("Active tracker count is less than zero");
                activeCount = 0;
            }
            if (context != null) {
                log.info("Stopping tracker server - no more active clients");
                context.close();
                context = null;
            }
        }
    }
}
