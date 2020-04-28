package sos.accumulo.monitor.tracker;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import sos.accumulo.monitor.data.AttemptInfo;
import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.data.RunnerHealth;
import sos.accumulo.monitor.data.ShardInfo;

public class RunnerTracker {
    
    private static final long MAX_FINISHED_SIZE = 1024 * 1024 * 100;
    private static final int MAX_ERRORS = 10;
    private static final RunnerTracker tracker = new RunnerTracker();
    private final AtomicLong finishedCount = new AtomicLong();
    private final Map<Long, QueryInfoDetail> running = new ConcurrentHashMap<>();
    private final Map<Long, QueryInfoDetail> finished = new ConcurrentHashMap<>();
    private final NavigableSet<QueryInfoDetail> finishedOrdered = new ConcurrentSkipListSet<>();
    private final NavigableSet<ErrorInfo> recentErrors = new ConcurrentSkipListSet<>();
    private final AtomicLong finishedSize = new AtomicLong();

    private RunnerTracker() { }

    public static RunnerTracker getInstance() {
        return tracker;
    }

    public void recordError(String error) {
        recentErrors.add(ErrorInfo.create(error));
        if (recentErrors.size() > MAX_ERRORS) {
            recentErrors.pollFirst();
        }
    }

    public void start(QueryInfoDetail detail) {
        running.put(detail.getInfo().getIndex(), detail);
    }

    public QueryInfoDetail getByIndex(long index) {
        QueryInfoDetail detail = finished.get(index);
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

    public QueryRunnerMatch findMatch(String queryString, String shard, long started) {
        QueryRunnerMatch match = new QueryRunnerMatch();
        for (Map<Long, QueryInfoDetail> shards : Arrays.asList(running, finished)) {
            for (QueryInfoDetail detail : shards.values()) {
                QueryInfo info = detail.getInfo();

                if (!info.getQueryString().equals(queryString)) {
                    continue;
                } else if (info.getStarted() > started || (info.getFinished() != 0 && info.getFinished() < started)) {
                    continue;
                }

                for (ShardInfo shardInfo : detail.getShards()) {
                    if (!shardInfo.getShard().equals(shard)) {
                        continue;
                    }
                    if (isMatch(shardInfo.getLatestAttempt(), started, match)) {
                        match.setQuery(info.getIndex());
                        match.setShard(shard);
                        if (match.getAttemptStarted() == started) {
                            return match;
                        }
                    }
                    for (AttemptInfo attempt : shardInfo.getFailedAttempts()) {
                        if (isMatch(attempt, started, match)) {
                            match.setQuery(info.getIndex());
                            match.setShard(shard);
                            if (match.getAttemptStarted() == started) {
                                return match;
                            }
                        }
                    }
                }
            }
        }
        return match;
    }

    private boolean isMatch(AttemptInfo attempt, long started, QueryRunnerMatch closest) {
        if (attempt.getStarted() == started) {
            closest.setFound(true);
            closest.setAttemptStarted(attempt.getStarted());
            closest.setAttemptServer(attempt.getServer());
            return true;
        } else if (attempt.getStarted() < started && (attempt.getIndexFinished() == 0 || attempt.getIndexFinished() > started)) {
            if (attempt.getStarted() > closest.getAttemptStarted()) {
                closest.setFound(true);
                closest.setAttemptStarted(attempt.getStarted());
                closest.setAttemptServer(attempt.getServer());
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void finish(long index) {
        QueryInfoDetail detail = running.remove(index);
        long size = finishedSize.addAndGet(detail.getSizeEstimate());
        finished.put(index, detail);
        finishedOrdered.add(detail);

        if (size > MAX_FINISHED_SIZE) {
            synchronized (this) {
                while (size > MAX_FINISHED_SIZE && finishedOrdered.size() > 0) {
                    QueryInfoDetail oldest = finishedOrdered.pollFirst();
                    finished.remove(oldest.getInfo().getIndex());
                    size -= oldest.getSizeEstimate();
                }
            }
        }
    }

    public Set<QueryInfo> getRunning() {
        Set<QueryInfo> running = new TreeSet<>();
        for (QueryInfoDetail info : this.running.values()) {
            running.add(info.getInfo());
        }
        return running;
    }

    public Set<QueryInfo> getFinished() {
        Set<QueryInfo> finished = new TreeSet<>();
        for (QueryInfoDetail info : this.finished.values()) {
            finished.add(info.getInfo());
        }
        return finished;
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

    public QueryRunnerStatus createStatus() {
        QueryRunnerStatus status = new QueryRunnerStatus();
        status.setHealth(RunnerHealth.create(getMostRecentError()));
        status.setFinished(finishedCount.get());
        status.setRunning(running.size());
        status.setRecentErrors(recentErrors);
        return status;
    }
}