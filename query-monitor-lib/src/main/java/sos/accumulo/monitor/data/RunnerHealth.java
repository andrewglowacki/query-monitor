package sos.accumulo.monitor.data;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class RunnerHealth {
    private static final long CREATION_TIME = System.currentTimeMillis();
    private long memoryMax;
    private long memoryUsed;
    private int threads;
    private long gcTime;
    private long gcCount;
    private long mostRecentError;
    private long upSince = CREATION_TIME;

    public static RunnerHealth create(long mostRecentError) {
        RunnerHealth health = new RunnerHealth();
        health.mostRecentError = mostRecentError;

        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        health.threads = root.activeCount();

        long eachMax = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() <= 0) {
                continue;
            }
            long each = bean.getCollectionTime() / bean.getCollectionCount();
            if (each > eachMax) {
                health.gcTime = bean.getCollectionTime();
                health.gcCount = bean.getCollectionCount();
            }
        }

        Runtime runtime = Runtime.getRuntime();
        health.memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        health.memoryMax = runtime.maxMemory();

        return health;
    }

    public long getUpSince() {
        return upSince;
    }

    public void setUpSince(long upSince) {
        this.upSince = upSince;
    }
    
    public long getMostRecentError() {
        return mostRecentError;
    }
    
    public void setMostRecentError(long mostRecentError) {
        this.mostRecentError = mostRecentError;
    }

    public long getMemoryMax() {
        return memoryMax;
    }

    public void setMemoryMax(long memoryMax) {
        this.memoryMax = memoryMax;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }
    
    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public long getGcTime() {
        return gcTime;
    }

    public void setGcTime(long gcTime) {
        this.gcTime = gcTime;
    }

    public long getGcCount() {
        return gcCount;
    }

    public void setGcCount(long gcCount) {
        this.gcCount = gcCount;
    }
    
}