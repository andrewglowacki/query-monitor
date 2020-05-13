package sos.accumulo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.util.HttpQuery;

public class MonitorRunnerTest {
    @Test
    public void webappRunsTest() throws IOException {
        System.setProperty("executor.file.path", "src/test/resources/webapp-test-executors");
        System.setProperty("server.address", "localhost");
        System.setProperty("server.port", "33333");
        MonitorRunner.main(new String[0]);

        // make sure it actually responds to a query
        GeneralStatus status = HttpQuery.normalQuery("http://localhost:33333/api/status", GeneralStatus.class);
        assertNotNull(status);
        assertEquals(0, status.getExecutors().size());
        assertEquals(0, status.getQueryRunners().size());
    }
}