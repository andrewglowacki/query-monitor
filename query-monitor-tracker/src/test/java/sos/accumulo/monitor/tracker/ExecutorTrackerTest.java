package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import sos.accumulo.monitor.data.ErrorInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.QueryPartInfo;

public class ExecutorTrackerTest {

    @Test
    public void createStatusTest() {
        ExecutorTracker tracker = new ExecutorTracker();

        long now = System.currentTimeMillis();
        ExecutorStatus status = tracker.createStatus();
        assertEquals(0, status.getFinished());
        assertTrue(status.getLastHeard() >= now);
        assertEquals(0, status.getRecentErrorCount());
        assertEquals(0, status.getRunning());
        assertNotNull(status.getHealth());
    }

    @Test
    public void createStatusDetailTest() {
        ExecutorTracker tracker = new ExecutorTracker();

        long now = System.currentTimeMillis();
        ExecutorStatusDetail detail = tracker.createStatusDetail();
        assertEquals(0, detail.getFinished());
        assertTrue(detail.getLastHeard() >= now);
        assertEquals(0, detail.getRecentErrorCount());
        assertEquals(0, detail.getRecentErrors().size());
        assertEquals(0, detail.getRunning());

        assertEquals(1, detail.getFinishedStats().size());
        assertEquals(1, detail.getResultStats().size());
        assertEquals(1, detail.getStatTimes().size());

        assertTrue(detail.getStatTimes().get(0) >= now);
        assertEquals(0L, detail.getFinishedStats().get(0).longValue());
        assertEquals(0L, detail.getResultStats().get(0).longValue());
        
        assertNotNull(detail.getHealth());
    }

    @Test
    public void recordErrorRolloverTest() throws Exception {
        ExecutorTracker tracker = new ExecutorTracker();
        long lastTime = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            tracker.recordError("error " + i);
            if (i < 10) {
                assertEquals(i + 1, tracker.createStatus().getRecentErrorCount());
            } else {
                assertEquals(10, tracker.createStatus().getRecentErrorCount());
            }
            Thread.sleep(2);
        }
        
        ExecutorStatusDetail detail = tracker.createStatusDetail();
        Set<ErrorInfo> errors = detail.getRecentErrors();

        assertEquals(10, detail.getRecentErrorCount());
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
        assertEquals(last.getTime(), detail.getHealth().getMostRecentError());
    }

    @Test
    public void runningTest() {
        ExecutorTracker tracker = new ExecutorTracker();

        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        ExecutorStatus status = tracker.createStatus();
        assertEquals(1, status.getRunning());
        assertEquals(0, status.getFinished());

        ExecutorStatus statusDetail = tracker.createStatusDetail();
        assertEquals(1, statusDetail.getRunning());
        assertEquals(0, statusDetail.getFinished());

        Set<ExecutorShardInfo> running = tracker.getRunning();
        assertEquals(1, running.size());

        assertEquals(detail.getInfo(), running.iterator().next());

        assertEquals(0, tracker.getFinished().size());

        assertEquals(detail, tracker.getByIndex(1));
    }

    @Test
    public void finishedTest() {
        ExecutorTracker tracker = new ExecutorTracker();

        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);
        tracker.finish(1);

        ExecutorStatus status = tracker.createStatus();
        assertEquals(0, status.getRunning());
        assertEquals(1, status.getFinished());

        ExecutorStatus statusDetail = tracker.createStatusDetail();
        assertEquals(0, statusDetail.getRunning());
        assertEquals(1, statusDetail.getFinished());

        assertTrue(detail.getInfo().getFinished() >= now);
        assertEquals(0, tracker.getRunning().size());

        Set<ExecutorShardInfo> finished = tracker.getFinished();
        assertEquals(1, finished.size());

        assertEquals(detail.getInfo(), finished.iterator().next());

        assertEquals(detail, tracker.getByIndex(1));
    }

    @Test
    public void doubleFinishTest() {

        ExecutorTracker tracker = new ExecutorTracker();

        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(1)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        // calling finish twice for the same index or non-existent indexes
        // shouldn't change the finished count or throw a NPE
        tracker.start(detail);
        tracker.finish(1);
        tracker.finish(1);
        tracker.finish(2);

        ExecutorStatus status = tracker.createStatus();
        assertEquals(0, status.getRunning());
        assertEquals(1, status.getFinished());
    }
    
    @Test
    public void statsTests() throws Exception {
        ExecutorTracker tracker = new ExecutorTracker();

        int currentCount = 0;
        int group = 0;
        for (int i = 0; i < 95; i++) {
            ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
                .setIndex(i)
                .setQueryString("this is a query")
                .setShard("0005_5")
                .setSourceServer("r3n04-node")
                .setStarted(System.currentTimeMillis())
                .setStartedQueueCount(3)
                .build(), new ArrayList<>());

            tracker.start(detail);

            detail.getInfo().setResults(10);

            tracker.finish(i);
            
            currentCount++;
            if (currentCount >= (10 + group)) {
                tracker.recordLatestStat();
                currentCount = 0;
                group++;
                Thread.sleep(2);
            }
        }

        ExecutorStatusDetail detail = tracker.createStatusDetail();

        assertEquals(0, detail.getRunning());
        assertEquals(95, detail.getFinished());

        final int EXP_COUNT = 8;

        assertEquals(EXP_COUNT, detail.getStatTimes().size());
        assertEquals(EXP_COUNT, detail.getFinishedStats().size());
        assertEquals(EXP_COUNT, detail.getResultStats().size());

        long last = 0;
        for (Long time : detail.getStatTimes()) {
            assertTrue("stat times should be increasing: " + time + " > " + last, time > last);
            last = time;
        }

        for (int i = 0; i < EXP_COUNT - 1; i++) {
            assertEquals(10 + i, detail.getFinishedStats().get(i).intValue());
            assertEquals(10 * (10 + i), detail.getResultStats().get(i).intValue());
        }

        assertEquals(currentCount, detail.getFinishedStats().get(EXP_COUNT - 1).intValue());
        assertEquals(10 * currentCount, detail.getResultStats().get(EXP_COUNT - 1).intValue());
    }

    @Test
    public void overflowStatsTest() throws Exception {
        ExecutorTracker tracker = new ExecutorTracker();

        for (int i = 0; i < ExecutorTracker.MAX_STAT_SAMPLES + 10; i++) {
            tracker.recordLatestStat();
            Thread.sleep(2);
        }

        ExecutorStatusDetail detail = tracker.createStatusDetail();
        assertEquals(ExecutorTracker.MAX_STAT_SAMPLES, detail.getFinishedStats().size());
        assertEquals(ExecutorTracker.MAX_STAT_SAMPLES, detail.getStatTimes().size());
        assertEquals(ExecutorTracker.MAX_STAT_SAMPLES, detail.getResultStats().size());
    }

    @Test
    public void finishMaxSizeTest() {
        // make sure the oldest finished items are removed after the max size is reached
        ExecutorTracker tracker = new ExecutorTracker();

        List<QueryPartInfo> parts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            parts.add(new QueryPartInfo());
        }
        for (QueryPartInfo part : parts) {
            part.setPartString("this is a part string");
            part.setChildren(new ArrayList<>(0));
        }
        
        long size = 0;
        int index = 0;
        long lastSize = 1;
        while (size < 2 * ExecutorTracker.MAX_FINISHED_SIZE) {
            ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
                .setIndex(index)
                .setQueryString("this is a query")
                .setShard("0005_5")
                .setSourceServer("r3n04-node")
                .setStarted(System.currentTimeMillis())
                .setStartedQueueCount(3)
                .build(), parts);

            tracker.start(detail);

            tracker.finish(index);

            lastSize = detail.getSizeEstimate();
            size += lastSize;

            // sanity check
            assertTrue(lastSize > 0);

            index++;
        }
        
        int expectedLeft = (int)Math.floor(ExecutorTracker.MAX_FINISHED_SIZE / lastSize);
        Set<ExecutorShardInfo> finished = tracker.getFinished();
        assertEquals(expectedLeft, finished.size());
        assertNotEquals(index, expectedLeft);

        ExecutorStatus status = tracker.createStatus();
        assertEquals(index, status.getFinished());
    }

    @Test
    public void findMatchNoneTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        assertNull(tracker.findMatch("this is a query", "0000", System.currentTimeMillis()));
    }

    @Test
    public void findMatchHitExactTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        ExecutorShardInfoDetail match = tracker.findMatch("this is a query", "0005_5", now);

        assertEquals(detail, match);
    }

    @Test
    public void findMatchHitCloseTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        ExecutorShardInfoDetail match = tracker.findMatch("this is a query", "0005_5", now - 10);

        assertEquals(detail, match);
    }

    @Test
    public void findMatchHitCloseFinishedTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);
        tracker.finish(0);

        ExecutorShardInfoDetail match = tracker.findMatch("this is a query", "0005_5", now - 10);
        
        assertEquals(detail, match);
    }

    @Test
    public void findMatchNoQueryMatchTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        assertNull(tracker.findMatch("this is a different query", "0005_5", now));
    }

    @Test
    public void findMatchNoShardMatchTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        assertNull(tracker.findMatch("this is a query", "0006_5", now));
    }

    @Test
    public void findMatchNoStartTimeMatchTest() {
        ExecutorTracker tracker = new ExecutorTracker();
        
        long now = System.currentTimeMillis();
        ExecutorShardInfoDetail detail = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
            .setIndex(0)
            .setQueryString("this is a query")
            .setShard("0005_5")
            .setSourceServer("r3n04-node")
            .setStarted(now)
            .setStartedQueueCount(3)
            .build(), new ArrayList<>());

        tracker.start(detail);

        assertNull(tracker.findMatch("this is a query", "0005_5", now + 10));
    }
}