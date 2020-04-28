package sos.accumulo.monitor.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.RunnerHealth;

public class ExecutorTracker {
    
    private static final long MAX_FINISHED_SIZE = 1024 * 1024 * 100;
    private static final int MAX_ERRORS = 10;
    private static final ExecutorTracker tracker = new ExecutorTracker();
    private static final int MAX_STAT_SAMPLES = 12 * 24;
    private final AtomicLong finishedCount = new AtomicLong();
    private final AtomicLong resultsTotalStat = new AtomicLong();
    private final AtomicLong finishedCountStat = new AtomicLong();
    private final Map<Long, ExecutorShardInfoDetail> running = new ConcurrentHashMap<>();
    private final Map<Long, ExecutorShardInfoDetail> finished = new ConcurrentHashMap<>();
    private final NavigableSet<ExecutorShardInfoDetail> finishedOrdered = new ConcurrentSkipListSet<>();
    private final NavigableSet<ErrorInfo> recentErrors = new ConcurrentSkipListSet<>();
    private final AtomicLong finishedSize = new AtomicLong();
    private final NavigableSet<ExecutorStatSample> stats = new ConcurrentSkipListSet<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("Executor Monitor Tracker Thread");
            thread.setDaemon(true);
            return thread;
        }
    });

    private ExecutorTracker() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.set(Calendar.SECOND, 0);
        now.add(Calendar.MINUTE, 5 - (now.get(Calendar.MINUTE) % 5));
        int waitTime = (int)((now.getTimeInMillis() - System.currentTimeMillis()) / 1000);
        if (waitTime < 0) {
            waitTime = 0;
        }
        executor.scheduleAtFixedRate(this::recordLatestStat, waitTime, 300, TimeUnit.SECONDS);
    }

    protected void recordLatestStat() {
        stats.add(new ExecutorStatSample(finishedCountStat.getAndSet(0), resultsTotalStat.getAndSet(0)));
        if (stats.size() >= MAX_STAT_SAMPLES) {
            stats.pollFirst();
        }
    }

    public static ExecutorTracker getInstance() {
        return tracker;
    }

    public void recordError(String error) {
        recentErrors.add(ErrorInfo.create(error));
        if (recentErrors.size() > MAX_ERRORS) {
            recentErrors.pollFirst();
        }
    }

    public void start(ExecutorShardInfoDetail detail) {
        running.put(detail.getInfo().getIndex(), detail);
    }

    public ExecutorShardInfoDetail getByIndex(long index) {
        ExecutorShardInfoDetail detail = finished.get(index);
        if (detail != null) {
            return detail;
        }

        detail = running.get(index);
        if (detail != null) {
            return detail;
        }

        // double check it didn't finish in between
        return finished.get(index);
    }

    public ExecutorShardInfoDetail findMatch(String queryString, String shard, long started) {
        long closestTime = Long.MAX_VALUE;
        ExecutorShardInfoDetail closest = null;
        for (Map<Long, ExecutorShardInfoDetail> shards : Arrays.asList(running, finished)) {
            for (ExecutorShardInfoDetail detail : shards.values()) {
                ExecutorShardInfo info = detail.getInfo();
                if (!info.getQueryString().equals(queryString)) {
                    continue;
                } else if (!info.getShard().equals(shard)) {
                    continue;
                }
                if (info.getStarted() >= started) {
                    if (info.getStarted() == started) {
                        return detail;
                    } else if (info.getStarted() < closestTime) {
                        closest = detail;
                        closestTime = info.getStarted();
                    }
                }
            }
        }
        return closest;
    }

    public void finish(long index) {
        ExecutorShardInfoDetail detail = running.remove(index);
        long size = finishedSize.addAndGet(detail.getSizeEstimate());
        finished.put(index, detail);
        finishedOrdered.add(detail);
        finishedCountStat.incrementAndGet();
        finishedCount.incrementAndGet();

        if (size > MAX_FINISHED_SIZE) {
            synchronized (this) {
                while (size > MAX_FINISHED_SIZE && finishedOrdered.size() > 0) {
                    ExecutorShardInfoDetail oldest = finishedOrdered.pollFirst();
                    finished.remove(oldest.getInfo().getIndex());
                    size -= oldest.getSizeEstimate();
                }
            }
        }
    }

    public Set<ExecutorShardInfo> getRunning() {
        Set<ExecutorShardInfo> running = new TreeSet<>();
        for (ExecutorShardInfoDetail info : this.running.values()) {
            running.add(info.getInfo());
        }
        return running;
    }

    public Set<ExecutorShardInfo> getFinished() {
        Set<ExecutorShardInfo> finished = new TreeSet<>();
        for (ExecutorShardInfoDetail info : this.finished.values()) {
            finished.add(info.getInfo());
        }
        return finished;
    }

    public ExecutorStatus createStatus() {
        ExecutorStatus status = new ExecutorStatus();
        status.setFinished(finishedCount.get());
        status.setRunning(running.size());
        status.setHealth(RunnerHealth.create(getMostRecentError()));
        status.setRecentErrorCount(recentErrors.size());
        return status;
    }

    private long getMostRecentError() {
        long mostRecentError = 0;
        if (recentErrors.size() > 0) {
            try {
                mostRecentError = recentErrors.last().getTime();
            } catch (NoSuchElementException ex) { }
        }
        return mostRecentError;
    }

    public ExecutorStatusDetail createStatusDetail() {
        ExecutorStatusDetail detail = new ExecutorStatusDetail();
        detail.setHealth(RunnerHealth.create(getMostRecentError()));
        detail.setFinished(finishedCount.get());
        detail.setRunning(running.size());
        detail.setRecentErrors(recentErrors);
        detail.setRecentErrorCount(recentErrors.size());

        List<Long> times = new ArrayList<>(stats.size() + 2);
        List<Long> results = new ArrayList<>(stats.size() + 2);
        List<Long> finished = new ArrayList<>(stats.size() + 2);

        for (ExecutorStatSample stat : stats) {
            times.add(stat.getTime());
            results.add(stat.getResults());
            finished.add(stat.getFinished());
        }

        times.add(System.currentTimeMillis());
        results.add(resultsTotalStat.get());
        finished.add(finishedCountStat.get());

        detail.setStatTimes(times);
        detail.setResultStats(results);
        detail.setFinishedStats(finished);
        
        return detail;
    }
}