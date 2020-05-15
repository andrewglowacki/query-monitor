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

public class MonitorRunnerProxyTest {
    private static final String executorFile = new File("src/test/resources/empty-executors").getAbsolutePath();
    private static final String BASE = "http://localhost:22325";

    private static volatile DaemonRunner ui = null;
    private static volatile DaemonRunner runner = null;

    @BeforeClass
    public static void setup() {
        try {
            ui = DaemonRunner.runMonitor(executorFile, 22325);
            ui.waitForWebReady();
            
            runner = DaemonRunner.runTracker("mode=runner", "announce.address=localhost:22325", "name=runner1", "scans=3", "proxy=true");

            runner.waitForTrackerReady();
        } catch (Throwable ex) {
            if (ui != null) {
                ui.close();
            }
            if (runner != null) {
                runner.close();
            }
            throw new RuntimeException(ex);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (ui != null) {
            ui.close();
        }
        if (runner != null) {
            runner.close();
        }
    }

    @Test
    public void generalStatusTest() throws IOException {
        long now = System.currentTimeMillis();

        GeneralStatus generalStatus = HttpQuery.normalQuery(BASE + "/api/status?refreshNow=true", GeneralStatus.class);
        assertNotNull(generalStatus);
        assertEquals(0, generalStatus.getExecutors().size());
        assertEquals(1, generalStatus.getQueryRunners().size());

        QueryRunnerStatus runner = generalStatus.getQueryRunners().get("runner1");
        assertEquals(1, runner.getRunning());
        assertEquals(0, runner.getFinished());
        assertTrue(runner.getLastHeard() >= now);
        assertEquals(0, runner.getRecentErrors().size());
        assertNotNull(runner.getHealth());
    }

    @Test
    public void runningTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, QueryInfo.class);
        
        Set<QueryInfo> running = HttpQuery.normalQuery(BASE + "/api/runner/runner1/running", type);
        assertEquals(1, running.size());
        assertEquals(1, running.iterator().next().getIndex());
    }

    @Test
    public void scansTest() throws IOException {
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
        
        List<AccumuloScanInfo> scans = HttpQuery.normalQuery(BASE + "/api/runner/runner1/scans", type);
        assertEquals(3, scans.size());
    }

    @Test
    public void getByIndexTest() throws IOException {
        QueryInfoDetail detail = HttpQuery.normalQuery(BASE + "/api/runner/runner1/query/1", QueryInfoDetail.class);

        assertEquals(1, detail.getInfo().getIndex());
        assertEquals("a proxy query string!", detail.getInfo().getQueryString());
    }

    public static Set<Long> longs(Long ... values) {
        Set<Long> set = new TreeSet<>();
        for (Long value : values) {
            set.add(value);
        }
        return set;
    }
}