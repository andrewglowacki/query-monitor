package sos.accumulo.monitor;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.util.HttpQuery;

public class StatusUpdater implements Runnable {

    private static final long THRESHOLD = 1000 * 60;
    private static final Logger log = LoggerFactory.getLogger(StatusUpdater.class);
    private final Map<String, String> queryRunners;
    private final ExecutorFileMonitor executorFileMonitor;
    private final int executorMonitorPort;
    private final ConcurrentHashMap<String, Long> lastAttempts = new ConcurrentHashMap<>();
    private final Map<String, ExecutorStatus> executorStatuses = new ConcurrentHashMap<>();
    private final Map<String, QueryRunnerStatus> queryRunnerStatuses = new ConcurrentHashMap<>();

    public StatusUpdater(ExecutorFileMonitor executorFileMonitor, Map<String, String> queryRunners,
            int executorMonitorPort) {
        this.executorFileMonitor = executorFileMonitor;
        this.queryRunners = queryRunners;
        this.executorMonitorPort = executorMonitorPort;
    }

    public GeneralStatus getGeneralStatus() {
        GeneralStatus status = new GeneralStatus();
        status.setExecutors(executorStatuses);
        status.setQueryRunners(queryRunnerStatuses);
        return status;
    }

    public void updateNow(String name, ExecutorStatusDetail status) {
        lastAttempts.put(name, System.currentTimeMillis());
        executorStatuses.put(name, new ExecutorStatus(status));
    }
    public void updateNow(String name, QueryRunnerStatus status) {
        lastAttempts.put(name, System.currentTimeMillis());
        queryRunnerStatuses.put(name, status);
    }

    @Override
    public void run() {
        try {
            for (Entry<String, String> runner : queryRunners.entrySet()) {
                if (shouldUpdate(runner.getKey())) {
                    updateQueryRunner(runner.getKey(), runner.getValue());
                }
            }
            for (String executor : executorFileMonitor.getExecutors()) {
                if (shouldUpdate(executor)) {
                    updateExecutor(executor);
                }
            }

            executorStatuses.keySet().retainAll(executorFileMonitor.getExecutors());
            queryRunnerStatuses.keySet().retainAll(queryRunners.keySet());
            
        } catch (Throwable ex) {
            log.error("Failed to update general statuses", ex);
        }
    }

    public void clearLastAttempts() {
        lastAttempts.clear();
    }

    protected boolean shouldUpdate(String name) {
        long now = System.currentTimeMillis();
        long threshold = now - THRESHOLD;

        Long lastAttempt = lastAttempts.get(name);
        if (lastAttempt != null && lastAttempt.longValue() > threshold) {
            return false;
        }
        // skip this one if another thread is already working on it.
        if (lastAttempt != lastAttempts.put(name, now)) {
            return false;
        }

        return true;
    }

    protected void updateQueryRunner(String name, String address) {
        try {
            QueryRunnerStatus status = HttpQuery.quickQuery("http://" + address + "/status", QueryRunnerStatus.class);
            queryRunnerStatuses.put(name, status);
        } catch (IOException ex) {
            log.warn("Failed to get runner status for " + name + " at " + address + " - " + ex.getMessage());
        }
    }
    protected void updateExecutor(String name) {
        try {
            String address = name + ":" + executorMonitorPort;
            ExecutorStatus status = HttpQuery.quickQuery("http://" + address + "/status", ExecutorStatus.class);
            executorStatuses.put(name, status);
        } catch (IOException ex) {
            log.warn("Failed to get executor status from " + name + ":" + executorMonitorPort + " - " + ex.getMessage());
        }
    }
    
}