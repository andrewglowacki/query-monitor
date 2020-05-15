package sos.accumulo.monitor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.util.HttpQuery;

public class MonitorRunnersTest {

    private static final String executorFile = new File("src/test/resources/empty-executors").getAbsolutePath();
    private static final String BASE = "http://localhost:22322";

    private static volatile DaemonRunner ui = null;
    private static volatile DaemonRunner runner1 = null;
    private static volatile DaemonRunner runner2 = null;

    @BeforeClass
    public static void setup() {
        try {
            ui = DaemonRunner.runMonitor(executorFile, 22322);
            ui.waitForWebReady();
            
            runner1 = DaemonRunner.runTracker("mode=runner", "announce.address=localhost:22322", "name=runner1", "running=1", "finished=3", "scans=2");
            runner2 = DaemonRunner.runTracker("mode=runner", "announce.address=localhost:22322", "name=runner2", "running=2", "finished=1", "scans=4");

            runner1.waitForTrackerReady();
            runner2.waitForTrackerReady();
        } catch (Throwable ex) {
            if (ui != null) {
                ui.close();
            }
            if (runner1 != null) {
                runner1.close();
            }
            if (runner2 != null) {
                runner2.close();
            }
            throw new RuntimeException(ex);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (ui != null) {
            ui.close();
        }
        if (runner1 != null) {
            runner1.close();
        }
        if (runner2 != null) {
            runner2.close();
        }
    }

    @Test
    public void generalStatusTest() throws IOException {
        long now = System.currentTimeMillis();

        GeneralStatus generalStatus = HttpQuery.normalQuery(BASE + "/api/status?refreshNow=true", GeneralStatus.class);
        assertNotNull(generalStatus);
        assertEquals(0, generalStatus.getExecutors().size());
        assertEquals(2, generalStatus.getQueryRunners().size());

        QueryRunnerStatus runner1 = generalStatus.getQueryRunners().get("runner1");
        assertEquals(1, runner1.getRunning());
        assertEquals(3, runner1.getFinished());
        assertTrue(runner1.getLastHeard() >= now);
        assertEquals(0, runner1.getRecentErrors().size());
        assertNotNull(runner1.getHealth());

        QueryRunnerStatus runner2 = generalStatus.getQueryRunners().get("runner2");
        assertEquals(2, runner2.getRunning());
        assertEquals(1, runner2.getFinished());
        assertTrue(runner2.getLastHeard() >= now);
        assertEquals(0, runner2.getRecentErrors().size());
        assertNotNull(runner2.getHealth());
    }

    @Test
    public void runningTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, QueryInfo.class);
        
        Set<QueryInfo> running1 = HttpQuery.normalQuery(BASE + "/api/runner/runner1/running", type);
        assertEquals(1, running1.size());
        assertEquals(4, running1.iterator().next().getIndex());

        Set<QueryInfo> running2 = HttpQuery.normalQuery(BASE + "/api/runner/runner2/running", type);
        assertEquals(2, running2.size());
        assertEquals(longs(2L, 3L), running2.stream()
            .map(QueryInfo::getIndex)
            .collect(Collectors.toSet()));
    }

    @Test
    public void finishedTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, QueryInfo.class);
        
        Set<QueryInfo> finished1 = HttpQuery.normalQuery(BASE + "/api/runner/runner1/finished", type);
        assertEquals(longs(1L, 2L, 3L), finished1.stream()
            .map(QueryInfo::getIndex)
            .collect(Collectors.toSet()));

        Set<QueryInfo> finished2 = HttpQuery.normalQuery(BASE + "/api/runner/runner2/finished", type);
        assertEquals(longs(1L), finished2.stream()
            .map(QueryInfo::getIndex)
            .collect(Collectors.toSet()));
    }

    @Test
    public void scansTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
        
        List<AccumuloScanInfo> scans1 = HttpQuery.normalQuery(BASE + "/api/runner/runner1/scans", type);
        assertEquals(2, scans1.size());

        List<AccumuloScanInfo> scans2 = HttpQuery.normalQuery(BASE + "/api/runner/runner2/scans", type);
        assertEquals(4, scans2.size());
    }

    @Test
    public void getByIndexTest() throws IOException {
        QueryInfoDetail detail1 = HttpQuery.normalQuery(BASE + "/api/runner/runner1/query/1", QueryInfoDetail.class);
        QueryInfoDetail detail2 = HttpQuery.normalQuery(BASE + "/api/runner/runner2/query/1", QueryInfoDetail.class);

        assertEquals(1, detail1.getInfo().getIndex());
        assertEquals(1, detail2.getInfo().getIndex());
        assertNotEquals(detail1.getInfo().getStarted(), detail2.getInfo().getStarted());
    }

    public static Set<Long> longs(Long ... values) {
        Set<Long> set = new TreeSet<>();
        for (Long value : values) {
            set.add(value);
        }
        return set;
    }
}