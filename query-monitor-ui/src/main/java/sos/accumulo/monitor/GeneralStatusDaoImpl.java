package sos.accumulo.monitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.data.QueryRunnerStatus;

@Repository
public class GeneralStatusDaoImpl implements GeneralStatusDao {

    private static final Logger log = LoggerFactory.getLogger(GeneralStatusDaoImpl.class);

    @Value("${executor.file.path}")
    private String executorFilePath;

    @Value("${executor.monitor.port:43214}")
    private int executorMonitorPort;

    @Value("${general.status.update.threads:10}")
    private int generalStatusUpdateThreads;

    private volatile ScheduledExecutorService executor = null;
    private volatile ExecutorFileMonitor executorFileMonitor;
    private volatile StatusUpdater statusUpdater;
    private final Map<String, String> queryRunners = new ConcurrentHashMap<>();

    @PostConstruct
    public void setup() {
        cleanup();
        executorFileMonitor = new ExecutorFileMonitor(executorFilePath);
        statusUpdater = new StatusUpdater(executorFileMonitor, queryRunners, executorMonitorPort);

        executor = Executors.newScheduledThreadPool(generalStatusUpdateThreads + 1);

        executor.scheduleWithFixedDelay(executorFileMonitor, 1, 1, TimeUnit.MINUTES);
        for (int i = 0; i < generalStatusUpdateThreads; i++) {
            executor.scheduleWithFixedDelay(statusUpdater, 0, 5, TimeUnit.MINUTES);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public GeneralStatus getGeneralStatus() {
        return statusUpdater.getGeneralStatus();
    }

    @Override
    public void updateAllNow() {
        statusUpdater.clearLastAttempts();
        statusUpdater.run();
    }

    @Override
    public void updateNow(String name, ExecutorStatusDetail status) {
        statusUpdater.updateNow(name, status);
    }

    @Override
    public void updateNow(String name, QueryRunnerStatus status) {
        statusUpdater.updateNow(name, status);
    }

    @Override
    public void register(String name, String address) {
        String oldAddress = queryRunners.put(name, address);
        if (oldAddress == null) {
            log.info("Registered new query runner: " + name + " at address: " + address);
        } else {
            log.info("Replaced query runner: " + name + " previously at address: " + oldAddress + " now at address: "
                    + address);
        }
    }

    @Override
    public String getAddress(String name) {
        String address = queryRunners.get(name);
        if (address != null) {
            return address;
        }

        if (!executorFileMonitor.getExecutors().contains(name)) {
            throw new IllegalArgumentException("No query runner or executor exists with name: " + name);
        }

        return name + ":" + executorMonitorPort;
    }

    @Override
    public Set<String> getRunnersOnServer(String server) {
        Set<String> names = new HashSet<>();
        for (Entry<String, String> entry : queryRunners.entrySet()) {
            if (entry.getValue().startsWith(server + ":")) {
                names.add(entry.getKey());
            }
        }
        return names;
    }
}