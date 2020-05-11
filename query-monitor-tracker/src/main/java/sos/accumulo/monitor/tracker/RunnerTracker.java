package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.AttemptInfo;
import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.data.RunnerHealth;
import sos.accumulo.monitor.data.ShardInfo;

@Profile({"TrackerModeRunner", "TrackerModeProxy"})
@Component
public class RunnerTracker {
    private static final Logger log = LoggerFactory.getLogger(RunnerTracker.class);
    protected static final long MAX_FINISHED_SIZE = 1024 * 1024 * 100;
    protected static final int MAX_ERRORS = 10;
    private final AtomicLong queryCounter = new AtomicLong();
    private final AtomicLong finishedCount = new AtomicLong();
    private final Map<Long, QueryInfoDetail> running = new ConcurrentHashMap<>();
    private final Map<Long, QueryInfoDetail> finished = new ConcurrentHashMap<>();
    private final NavigableSet<QueryInfoDetail> finishedOrdered = new ConcurrentSkipListSet<>();
    private final NavigableSet<ErrorInfo> recentErrors = new ConcurrentSkipListSet<>();
    private final AtomicLong finishedSize = new AtomicLong();
    private final Map<Long, RegisteredProxy> proxiedRunning = new ConcurrentHashMap<>();

    // initialize for testing purposes
    @Autowired
    private ProxyDao proxyDao = new ProxyDaoOrigin();

    public ProxyQuery startProxyQuery(QueryInfo.Builder builder) {
        try {
            return new ProxyQuery(this, proxyDao.startProxyQuery(), builder);
        } catch (IOException ex) { 
            log.warn("Failed to register proxy query - query will not be tracked", ex);
            return new ProxyQuery(null, -1, builder);
        }
    }

    /**
     * Called automatically when the proxy query is closed
     */
    public void finishProxyQuery(ProxyQuery query) {
        try {
            proxyDao.finishProxyQuery(query);
        } catch (IOException ex) { 
            log.warn("Failed to finish proxy query - query will not be dangling", ex);
        }
    }

    public void recordError(String error) {
        if (proxyDao.recordError(error)) {
            return;
        }
        recentErrors.add(ErrorInfo.create(error));
        if (recentErrors.size() > MAX_ERRORS) {
            recentErrors.pollFirst();
        }
    }

    public long nextQueryIndex() {
        return queryCounter.incrementAndGet();
    }

    public void start(QueryInfoDetail detail) {
        long index = detail.getInfo().getIndex();
        if (index >= 0) {
            running.put(detail.getInfo().getIndex(), detail);
        }
    }

    public long registerProxy(String address, String proxyId) {
        long queryId = nextQueryIndex();
        proxiedRunning.put(queryId, new RegisteredProxy(queryId, address, proxyId));
        return queryId;
    }

    public void proxyFinished(QueryInfoDetail detail) {
        if (proxiedRunning.remove(detail.getInfo().getIndex()) != null) {
            start(detail);
            finish(detail.getInfo().getIndex());
        }
    }

    public void terminateAll(String proxyId) {
        proxiedRunning.values().stream()
            .filter(proxy -> proxy.getId().equals(proxyId))
            .map(proxy -> new QueryInfoDetail(new QueryInfo.Builder()
                .setError("Proxy runner crashed without reporting status")
                .setFinished(System.currentTimeMillis())
                .setIndex(proxy.getQueryIndex())
                .setOriginThreadName("unknown")
                .setQueryString("unknown")
                .setResultSize(0)
                .setResults(0)
                .setShardsComplete(0)
                .setShardsTotal(0)
                .setStarted(proxy.getStarted())
                .build(), new ArrayList<>(0)))
            .forEach(this::proxyFinished);
    }

    /** For testing purposes only */
    protected void setProxyDao(ProxyDao proxyDao) {
        this.proxyDao = proxyDao;
    }
    protected Map<Long, RegisteredProxy> getProxiedRunning() {
        return proxiedRunning;
    }

    public List<AccumuloScanInfo> getProxyScans() {
        List<AccumuloScanInfo> scans = new ArrayList<>();
        for (RegisteredProxy proxy : proxiedRunning.values()) {
            try {
                scans.addAll(proxyDao.getRunningScans(proxy.getAddress()));
            } catch (IOException ex) {
                log.error("Failed to get scans from proxy: " + proxy.getAddress(), ex);
            }
        }
        return scans;
    }

    public QueryInfoDetail getByIndex(long index) throws IOException {
        QueryInfoDetail detail = finished.get(index);
        if (detail != null) {
            return detail;
        }

        detail = running.get(index);
        if (detail != null) {
            return detail;
        }

        RegisteredProxy proxy = proxiedRunning.get(index);
        if (proxy != null) {
            detail = proxyDao.getQueryDetail(proxy.getAddress(), index);
            if (detail != null) {
                return detail;
            }
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

        if (proxiedRunning.size() > 0) {
            match = proxyDao.findMatch(queryString, shard, started, proxiedRunning.values(), match);
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

    public void finish(QueryInfoDetail detail) {
        finish(detail.getInfo().getIndex());
    }
    public void finish(long index) {
        QueryInfoDetail detail = running.remove(index);
        if (detail == null) {
            log.warn("Query Info with index " + index + " is not running");
            return;
        }
        long size = finishedSize.addAndGet(detail.getSizeEstimate());
        finished.put(index, detail);
        finishedOrdered.add(detail);
        finishedCount.incrementAndGet();

        if (size > MAX_FINISHED_SIZE) {
            synchronized (this) {
                long sizeOrig = size = finishedSize.get();
                while (size > MAX_FINISHED_SIZE && finishedOrdered.size() > 0) {
                    QueryInfoDetail oldest = finishedOrdered.pollFirst();
                    finished.remove(oldest.getInfo().getIndex());
                    size -= oldest.getSizeEstimate();
                }
                finishedSize.addAndGet(size - sizeOrig);
            }
        }
    }

    public Set<QueryInfo> getRunning() {
        Set<QueryInfo> running = new TreeSet<>();
        for (QueryInfoDetail info : this.running.values()) {
            running.add(info.getInfo());
        }
        if (proxiedRunning.size() > 0) {
            for (RegisteredProxy proxy : proxiedRunning.values()) {
                try {
                    running.addAll(proxyDao.getRunningQueries(proxy.getAddress()));
                } catch (IOException ex) {
                    log.error("Failed to get running queries from proxy: " + proxy.getAddress(), ex);
                }
            }
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
        status.setRunning(running.size() + proxiedRunning.size());
        status.setRecentErrors(recentErrors);
        return status;
    }
}