package sos.accumulo.monitor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.util.HttpQuery;

public class FullManualTest {
    private static final String executorFile = new File("src/test/resources/localhost-executors").getAbsolutePath();

    @Ignore
    @Test
    public void test() throws IOException {
        try (DaemonRunner ui = DaemonRunner.runMonitor(executorFile, 8080)) {
            ui.waitForWebReady();
            long start = System.currentTimeMillis();
            try (DaemonRunner runner = DaemonRunner.runTracker("mode=runner", "name=test-runner", "announce.address=localhost:8080", "manualTest=true", "manualStart=" + start);
                DaemonRunner executor = DaemonRunner.runTracker("mode=executor", "port=43214", "manualTest=true", "manualStart=" + start)) {
                
                runner.waitForTrackerReady();
                executor.waitForTrackerReady();

                GeneralStatus generalStatus = HttpQuery.normalQuery("http://localhost:8080/api/status?refreshNow=true", GeneralStatus.class);
                assertNotNull(generalStatus);
                assertEquals(1, generalStatus.getExecutors().size());
                assertEquals(1, generalStatus.getQueryRunners().size());

                // set breakpoint here
                System.err.println("Test Done!");
            }
        }
    }
}