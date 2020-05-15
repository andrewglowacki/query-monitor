package sos.accumulo.monitor.tracker;

import java.net.Inet4Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TrackerServer
{
    private static final Logger log = LoggerFactory.getLogger(TrackerServer.class);
    private static int activeCount = 0;
    private static volatile ConfigurableApplicationContext context;
    private static TrackerMode mode;

    public static ExecutorTracker getExecutorTracker() {
        return context.getBean(ExecutorTracker.class);
    }

    public static RunnerTracker getRunnerTracker() {
        return context.getBean(RunnerTracker.class);
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    public static synchronized TrackerHandle startRunnerProxy(String originProxyAddress, String proxyId) {
        if (context != null) {
            assertMode(TrackerMode.Proxy);
            return new TrackerHandle(getTrackerAddress());
        }
        System.setProperty("origin.proxy.address", originProxyAddress);
        System.setProperty("proxy.id", proxyId);
        System.setProperty("server.address", "" + Inet4Address.getLoopbackAddress().getHostAddress());
		return startServer(0, TrackerMode.Proxy);
    }

    public static synchronized TrackerHandle startExecutorTracker(int port) {
        if (context != null) {
            assertMode(TrackerMode.Executor);
            return new TrackerHandle(getTrackerAddress());
        }
        return startServer(port, TrackerMode.Executor);
    }
    public static synchronized TrackerHandle startRunnerTracker(String announceAddress, String name) {
        if (context != null) {
            assertMode(TrackerMode.Runner);
            return new TrackerHandle(getTrackerAddress());
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

        return startServer(0, TrackerMode.Runner);
    }

    private static void assertMode(TrackerMode expected) {
        if (mode != expected) {
            throw new IllegalArgumentException("Tracker server is already running in mode: " + mode);
        }
    }

    private static String getTrackerAddress() {
        return context.getBean(TrackerAddress.class).get();
    }
    
    protected static synchronized TrackerHandle startServer(int port, TrackerMode mode) {
        if (activeCount != 0) {
            log.warn("Tracker server active count is non-zero, however no context exists.");
        }
        log.info("Creating tracker server with mode: " + mode);
        System.setProperty("spring.profiles.active", "TrackerMode" + mode);
        System.setProperty("server.port", "" + port);
        context = SpringApplication.run(TrackerServer.class);
        activeCount = 0;
        TrackerServer.mode = mode;
		return new TrackerHandle(getTrackerAddress());
    }

    protected static synchronized int getActiveCount() {
        return activeCount;
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
