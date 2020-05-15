package sos.accumulo.monitor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.util.HttpQuery;

public class MonitorExecutorsTest {

    private static final String executorFile = new File("src/test/resources/localhost-executors").getAbsolutePath();
    private static final String BASE = "http://localhost:22323";

    private static volatile DaemonRunner ui = null;
    private static volatile DaemonRunner executor = null;

    @BeforeClass
    public static void setup() {
        try {
            ui = DaemonRunner.runMonitor(executorFile, 22323);
            ui.waitForWebReady();
            
            executor = DaemonRunner.runTracker("mode=executor", "port=43214", "running=1", "finished=3", "scans=2");

            executor.waitForTrackerReady();
        } catch (Throwable ex) {
            if (ui != null) {
                ui.close();
            }
            if (executor != null) {
                executor.close();
            }
            throw new RuntimeException(ex);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (ui != null) {
            ui.close();
        }
        if (executor != null) {
            executor.close();
        }
    }

    @Test
    public void generalStatusTest() throws IOException {
        long now = System.currentTimeMillis();

        GeneralStatus generalStatus = HttpQuery.normalQuery(BASE + "/api/status?refreshNow=true", GeneralStatus.class);
        assertNotNull(generalStatus);
        assertEquals(1, generalStatus.getExecutors().size());
        assertEquals(0, generalStatus.getQueryRunners().size());

        ExecutorStatus executor = generalStatus.getExecutors().get("localhost");
        assertEquals(1, executor.getRunning());
        assertEquals(3, executor.getFinished());
        assertTrue(executor.getLastHeard() >= now);
        assertEquals(0, executor.getRecentErrorCount());
        assertNotNull(executor.getHealth());
    }

    @Test
    public void runningTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, ExecutorShardInfo.class);
        
        Set<ExecutorShardInfo> running = HttpQuery.normalQuery(BASE + "/api/executor/localhost/running", type);
        assertEquals(1, running.size());
        assertEquals(3, running.iterator().next().getIndex());
    }

    @Test
    public void finishedTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, ExecutorShardInfo.class);
        
        Set<ExecutorShardInfo> finished = HttpQuery.normalQuery(BASE + "/api/executor/localhost/finished", type);
        assertEquals(longs(0L, 1L, 2L), finished.stream()
            .map(ExecutorShardInfo::getIndex)
            .collect(Collectors.toSet()));
    }

    @Test
    public void scansTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
        
        List<AccumuloScanInfo> scans = HttpQuery.normalQuery(BASE + "/api/executor/localhost/scans", type);
        assertEquals(2, scans.size());
    }

    @Test
    public void getByIndexTest() throws IOException {
        ExecutorShardInfoDetail detail = HttpQuery.normalQuery(BASE + "/api/executor/localhost/shard/1", ExecutorShardInfoDetail.class);
        assertEquals(1, detail.getInfo().getIndex());
    }

    public static Set<Long> longs(Long ... values) {
        Set<Long> set = new TreeSet<>();
        for (Long value : values) {
            set.add(value);
        }
        return set;
    }
    
}