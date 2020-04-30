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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.AttemptInfo;
import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.data.RunnerHealth;
import sos.accumulo.monitor.data.ShardInfo;
import sos.accumulo.monitor.util.HttpQuery;

public class RunnerTracker {
    private static final Logger log = LoggerFactory.getLogger(RunnerTracker.class);
    private static final JavaType SCAN_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
    private static final JavaType QUERY_INFO_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, QueryInfo.class);
    private static final long MAX_FINISHED_SIZE = 1024 * 1024 * 100;
    private static final int MAX_ERRORS = 10;
    private static final RunnerTracker tracker = new RunnerTracker();
    private final AtomicLong queryCounter = new AtomicLong();
    private final AtomicLong finishedCount = new AtomicLong();
    private final Map<Long, QueryInfoDetail> running = new ConcurrentHashMap<>();
    private final Map<Long, QueryInfoDetail> finished = new ConcurrentHashMap<>();
    private final NavigableSet<QueryInfoDetail> finishedOrdered = new ConcurrentSkipListSet<>();
    private final NavigableSet<ErrorInfo> recentErrors = new ConcurrentSkipListSet<>();
    private final AtomicLong finishedSize = new AtomicLong();
    private final Map<Long, String> proxiedRunning = new ConcurrentHashMap<>();
    private volatile String originProxyServer;
    private volatile String localProxyServer;

    private RunnerTracker() {
    }

    public static RunnerTracker getInstance() {
        return tracker;
    }

    public void setProxyServer(String originProxyServer, String localProxyServer) {
        this.originProxyServer = originProxyServer;
        this.localProxyServer = localProxyServer;
    }

    public ProxyQuery startProxyQuery(QueryInfo.Builder builder) {
        if (localProxyServer == null || localProxyServer.isEmpty() || originProxyServer == null || originProxyServer.isEmpty()) {
            log.error("No local proxy server is registered - cannot start proxy query");
            return new ProxyQuery(-1, builder);
        }
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicHeader("address", localProxyServer));
            return new ProxyQuery(HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/start", params, Long.class), builder);
        } catch (IOException ex) { 
            log.warn("Failed to register proxy query - query will not be tracked", ex);
            return new ProxyQuery(-1, builder);
        }
    }

    /**
     * Called automatically when the proxy query is closed
     */
    public void finishProxyQuery(ProxyQuery query) {
        try {
            HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/finished", query.getDetail());
        } catch (IOException ex) { 
            log.warn("Failed to finish proxy query - query will not be dangling", ex);
        }
    }

    public void recordError(String error) {
        if (originProxyServer != null) {
            try {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicHeader("error", error));
                HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/error", params);
            } catch (IOException ex) {
                log.warn("Failed to report error to proxy origin server", ex);
            }
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
        running.put(detail.getInfo().getIndex(), detail);
    }

    public long registerProxy(String address) {
        long queryId = nextQueryIndex();
        proxiedRunning.put(queryId, address);
        return queryId;
    }

    public void proxyFinished(QueryInfoDetail detail) {
        if (proxiedRunning.remove(detail.getInfo().getIndex()) != null) {
            start(detail);
            finish(detail.getInfo().getIndex());
        }
    }

    public List<AccumuloScanInfo> getProxyScans() {
        List<AccumuloScanInfo> scans = new ArrayList<>();
        for (String proxyAddress : proxiedRunning.values()) {
            try {
                scans.addAll(HttpQuery.normalQuery("http://" + proxyAddress + "/scans", SCAN_LIST));
            } catch (IOException ex) {
                log.error("Failed to get scans from proxy: " + proxyAddress, ex);
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

        String proxyAddress = proxiedRunning.get(index);
        if (proxyAddress != null) {
            detail = HttpQuery.normalQuery("http://" + proxyAddress + "/query/" + index, QueryInfoDetail.class);
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
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicHeader("queryString", queryString));
            params.add(new BasicHeader("shard", shard));
            params.add(new BasicHeader("started", "" + started));
            for (String proxyAddress : proxiedRunning.values()) {
                try {
                    QueryRunnerMatch thisMatch = HttpQuery.normalPostQuery("http://" + proxyAddress + "/find", params, QueryRunnerMatch.class);
                    if (thisMatch.isFound() && thisMatch.getAttemptStarted() > match.getAttemptStarted()) {
                        match = thisMatch;
                        if (match.getAttemptStarted() == started) {
                            return match;
                        }
                    }
                } catch (IOException ex) {
                    log.error("Failed to find match from proxy: " + proxyAddress, ex);
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
        if (proxiedRunning.size() > 0) {
            for (String proxyAddress : proxiedRunning.values()) {
                try {
                    running.addAll(HttpQuery.normalQuery("http://" + proxyAddress + "/running", QUERY_INFO_LIST));
                } catch (IOException ex) {
                    log.error("Failed to get running queries from proxy: " + proxyAddress, ex);
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