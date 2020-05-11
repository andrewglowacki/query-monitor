package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.AttemptInfo;
import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.data.ShardInfo;

public class RunnerTrackerTest {

    @Test
    public void createStatusTest() {
        RunnerTracker tracker = new RunnerTracker();

        long now = System.currentTimeMillis();
        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(0, status.getFinished());
        assertTrue(status.getLastHeard() >= now);
        assertEquals(0, status.getRecentErrors().size());
        assertEquals(0, status.getRunning());
        assertNotNull(status.getHealth());
    }

    @Test
    public void recordErrorRolloverTest() throws Exception {
        RunnerTracker tracker = new RunnerTracker();
        long lastTime = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            tracker.recordError("error " + i);
            if (i < 10) {
                assertEquals(i + 1, tracker.createStatus().getRecentErrors().size());
            } else {
                assertEquals(10, tracker.createStatus().getRecentErrors().size());
            }
            Thread.sleep(2);
        }

        QueryRunnerStatus status = tracker.createStatus();
        Set<ErrorInfo> errors = status.getRecentErrors();

        assertEquals(10, status.getRecentErrors().size());
        assertEquals(10, errors.size());
        ErrorInfo last = null;
        int index = 10;
        for (ErrorInfo error : errors) {
            assertTrue(error.getTime() >= lastTime);
            assertEquals("error " + index, error.getError());
            lastTime = error.getTime();
            index++;
            last = error;
        }
        assertEquals(last.getTime(), status.getHealth().getMostRecentError());
    }

    @Test
    public void runningTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        long now = System.currentTimeMillis();
        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), new ArrayList<>());

        tracker.start(detail);

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(1, status.getRunning());
        assertEquals(0, status.getFinished());

        Set<QueryInfo> running = tracker.getRunning();
        assertEquals(1, running.size());

        assertEquals(detail.getInfo(), running.iterator().next());

        assertEquals(0, tracker.getFinished().size());

        assertEquals(detail, tracker.getByIndex(1));
    }

    @Test
    public void finishedTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        long now = System.currentTimeMillis();
        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setFinished(now)
            .setShardsComplete(108)
            .setShardsTotal(108)
            .build(), new ArrayList<>());

        tracker.start(detail);
        tracker.finish(1);

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(0, status.getRunning());
        assertEquals(1, status.getFinished());

        assertTrue(detail.getInfo().getFinished() >= now);
        assertEquals(0, tracker.getRunning().size());

        Set<QueryInfo> finished = tracker.getFinished();
        assertEquals(1, finished.size());

        assertEquals(detail.getInfo(), finished.iterator().next());

        assertEquals(detail, tracker.getByIndex(1));
    }

    @Test
    public void doubleFinishTest() {
        RunnerTracker tracker = new RunnerTracker();

        long now = System.currentTimeMillis();
        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), new ArrayList<>());

        // calling finish twice for the same index or non-existent indexes
        // shouldn't change the finished count or throw a NPE
        tracker.start(detail);
        tracker.finish(1);
        tracker.finish(1);
        tracker.finish(2);

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(0, status.getRunning());
        assertEquals(1, status.getFinished());
    }
    
    @Test
    public void finishMaxSizeTest() {
        // make sure the oldest finished items are removed after the max size is reached
        RunnerTracker tracker = new RunnerTracker();

        List<ShardInfo> shards = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ShardInfo shard = new ShardInfo();
            int num = i % 10;
            int piece = i / 10;
            shard.setShard("000" + num + "_" + piece);

            AttemptInfo attempt = new AttemptInfo();
            attempt.setServer("r01n38-node");
            shard.setLatestAttempt(attempt);
            shards.add(shard);
        }
        
        long size = 0;
        int index = 0;
        long lastSize = 1;
        while (size < 2 * RunnerTracker.MAX_FINISHED_SIZE) {
            QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
                .setIndex(index)
                .setQueryString("this is a query")
                .setStarted(System.currentTimeMillis())
                .setShardsComplete(100)
                .setShardsTotal(100)
                .build(), shards);

            tracker.start(detail);

            tracker.finish(index);

            lastSize = detail.getSizeEstimate();
            size += lastSize;

            // sanity check
            assertTrue(lastSize > 0);

            index++;
        }
        
        int expectedLeft = (int)Math.floor(RunnerTracker.MAX_FINISHED_SIZE / lastSize);
        Set<QueryInfo> finished = tracker.getFinished();
        assertEquals(expectedLeft, finished.size());
        assertNotEquals(index, expectedLeft);

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(index, status.getFinished());
    }

    @Test
    public void findMatchNoneTest() {
        RunnerTracker tracker = new RunnerTracker();
        QueryRunnerMatch match = tracker.findMatch("this is a query", "0000", System.currentTimeMillis());
        assertFalse(match.isFound());
    }

    @Test
    public void findMatchHitExactTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now - 5);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now - 5);

        assertEquals("r1n13-node", match.getAttemptServer());
        assertEquals(now - 5, match.getAttemptStarted());
        assertNull(match.getName());
        assertEquals("0005_5", match.getShard());
        assertEquals(1, match.getQuery());
    }

    @Test
    public void findMatchHitCloseTimeTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now - 5);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now);

        assertEquals("r1n13-node", match.getAttemptServer());
        assertEquals(now - 5, match.getAttemptStarted());
        assertNull(match.getName());
        assertEquals("0005_5", match.getShard());
        assertEquals(1, match.getQuery());
    }

    @Test
    public void findMatchHitOnFailedAttemptTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo failed = new AttemptInfo();
        failed.setServer("r1n13-node");
        failed.setStarted(now - 5);
        failed.setFinished(now - 1);
        shard.getFailedAttempts().add(failed);

        AttemptInfo latest = new AttemptInfo();
        latest.setServer("r3n14-node");
        latest.setStarted(now);
        shard.setLatestAttempt(latest);

        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now - 2);

        assertEquals("r1n13-node", match.getAttemptServer());
        assertEquals(now - 5, match.getAttemptStarted());
        assertNull(match.getName());
        assertEquals("0005_5", match.getShard());
        assertEquals(1, match.getQuery());
    }

    @Test
    public void findMatchMissQueryTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a different query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now);
        assertFalse(match.isFound());
    }

    @Test
    public void findMatchMissTimeFinishedTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now - 5);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setFinished(now)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now + 1);
        assertFalse(match.isFound());
    }

    @Test
    public void findMatchMissTimeIncompleteTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0005_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now + 10);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now);
        assertFalse(match.isFound());
    }

    @Test
    public void findMatchMissShardTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        List<ShardInfo> shards = new ArrayList<>();
        
        ShardInfo shard = new ShardInfo();
        shard.setShard("0004_5");

        long now = System.currentTimeMillis();

        AttemptInfo attempt = new AttemptInfo();
        attempt.setServer("r1n13-node");
        attempt.setStarted(now);

        shard.setLatestAttempt(attempt);
        shards.add(shard);

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build(), shards);

        tracker.start(detail);

        QueryRunnerMatch match = tracker.findMatch("this is a query", "0005_5", now);
        assertFalse(match.isFound());
    }

    @Test
    public void proxyOriginStartTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        long now = System.currentTimeMillis();

        long queryId = tracker.registerProxy("test-address", "test-id");
        assertEquals(1, queryId);

        assertEquals(1, tracker.getProxiedRunning().size());
        RegisteredProxy proxy = tracker.getProxiedRunning().get(1L);

        assertNotNull(proxy);
        assertEquals("test-address", proxy.getAddress());
        assertEquals("test-id", proxy.getId());
        assertEquals(1, proxy.getQueryIndex());
        assertTrue(proxy.getStarted() >= now);

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(1, status.getRunning());
        assertEquals(0, status.getFinished());
    }

    @Test
    public void proxyOriginFinishTest() {
        RunnerTracker tracker = new RunnerTracker();
        
        long now = System.currentTimeMillis();

        long queryId = tracker.registerProxy("test-address", "test-id");
        assertEquals(1, queryId);

        assertEquals(1, tracker.getProxiedRunning().size());

        QueryInfo info = new QueryInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setStarted(now - 10)
            .setShardsComplete(5)
            .setShardsTotal(108)
            .build();
        tracker.proxyFinished(new QueryInfoDetail(info, new ArrayList<>()));
                
        assertEquals(0, tracker.getProxiedRunning().size());

        QueryRunnerStatus status = tracker.createStatus();
        assertEquals(0, status.getRunning());
        assertEquals(1, status.getFinished());

        assertEquals(1, tracker.getFinished().size());
        assertEquals(info, tracker.getFinished().iterator().next());
    }

    @Test
    public void terminateProxiesTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        long queryId1 = tracker.registerProxy("test-address", "test-id");
        tracker.registerProxy("test-address", "test-id2");
        long queryId3 = tracker.registerProxy("test-address", "test-id");

        // sanity check
        assertNotEquals(queryId1, queryId3);

        assertEquals(3, tracker.getProxiedRunning().size());
        assertEquals(3, tracker.createStatus().getRunning());
        assertEquals(0, tracker.createStatus().getFinished());

        tracker.terminateAll("test-id");
        
        assertEquals(1, tracker.getProxiedRunning().size());
        assertEquals(1, tracker.createStatus().getRunning());
        assertEquals(2, tracker.createStatus().getFinished());

        assertEquals(2, tracker.getFinished().size());
        QueryInfoDetail query1 = tracker.getByIndex(queryId1);
        assertEquals(queryId1, query1.getInfo().getIndex());
        assertEquals("Proxy runner crashed without reporting status", query1.getInfo().getError());

        QueryInfoDetail query3 = tracker.getByIndex(queryId3);
        assertEquals(queryId3, query3.getInfo().getIndex());
        assertEquals("Proxy runner crashed without reporting status", query3.getInfo().getError());
        
    }

    @Test
    public void getProxyScansTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        AccumuloScanInfo scan1 = new AccumuloScanInfo();
        AccumuloScanInfo scan2 = new AccumuloScanInfo();

        ProxyDao dao = mock(ProxyDao.class);
        tracker.setProxyDao(dao);
        when(dao.getRunningScans(eq("test-address1"))).thenReturn(Arrays.asList(scan1));
        when(dao.getRunningScans(eq("test-address2"))).thenReturn(Arrays.asList(scan2));
        
        tracker.registerProxy("test-address1", "test-id1");
        tracker.registerProxy("test-address2", "test-id2");
        
        List<AccumuloScanInfo> scans = tracker.getProxyScans();

        assertEquals(2, scans.size());
        assertTrue(scans.get(0) == scan1 || scans.get(0) == scan2);
        assertTrue(scans.get(1) == scan1 || scans.get(1) == scan2);
        assertNotEquals(scans.get(0), scans.get(1));

        verify(dao, times(1)).getRunningScans(eq("test-address1"));
        verify(dao, times(1)).getRunningScans(eq("test-address2"));
        verify(dao, times(2)).getRunningScans(anyString());
    }

    @Test
    public void getProxyRunningTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        ProxyDao dao = mock(ProxyDao.class);
        tracker.setProxyDao(dao);

        long id1 = tracker.registerProxy("test-address1", "test-id1");
        long id2 = tracker.registerProxy("test-address2", "test-id2");

        when(dao.getRunningQueries(eq("test-address1")))
            .thenReturn(Arrays.asList(
                new QueryInfo.Builder()
                    .setIndex(id1)
                    .setQueryString("query 3")
                    .setShardsTotal(100)
                    .build()));
        when(dao.getRunningQueries(eq("test-address2")))
            .thenReturn(Arrays.asList(
                new QueryInfo.Builder()
                    .setIndex(id2)
                    .setQueryString("query 4")
                    .setShardsTotal(100)
                    .build()));

        Set<QueryInfo> running = tracker.getRunning();

        assertEquals(2, running.size());

        Iterator<QueryInfo> iter = running.iterator();
        assertEquals(id1, iter.next().getIndex());
        assertEquals(id2, iter.next().getIndex());

        verify(dao, times(1)).getRunningQueries(eq("test-address1"));
        verify(dao, times(1)).getRunningQueries(eq("test-address2"));
        verify(dao, times(2)).getRunningQueries(anyString());
    }

    @Test
    public void getProxyByIdTest() throws IOException {
        RunnerTracker tracker = new RunnerTracker();

        ProxyDao dao = mock(ProxyDao.class);
        tracker.setProxyDao(dao);

        long queryId = tracker.registerProxy("test-address1", "test-id1");

        when(dao.getQueryDetail(eq("test-address1"), eq(queryId)))
            .thenReturn(new QueryInfoDetail(
                new QueryInfo.Builder()
                    .setIndex(7)
                    .setQueryString("a query string")
                    .setShardsTotal(44)
                    .build(), new ArrayList<>()));

        QueryInfoDetail detail = tracker.getByIndex(queryId);

        assertNotNull(detail);
        assertEquals(7, detail.getInfo().getIndex());

        verify(dao, times(1)).getQueryDetail(eq("test-address1"), eq(queryId));
        verify(dao, times(1)).getQueryDetail(anyString(), anyLong());
    }

    @Test
    public void proxyFindMatch() {
        RunnerTracker tracker = new RunnerTracker();

        ProxyDao dao = mock(ProxyDao.class);
        tracker.setProxyDao(dao);

        tracker.registerProxy("test-address1", "test-id1");

        long now = System.currentTimeMillis();

        tracker.findMatch("a test query", "0005_1", now);

        verify(dao, times(1)).findMatch(eq("a test query"), eq("0005_1"), eq(now), eq(tracker.getProxiedRunning().values()), nullable(QueryRunnerMatch.class));
    }
}