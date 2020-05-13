package sos.accumulo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.test.controller.TestController;
import sos.accumulo.monitor.util.HttpQuery;

@ActiveProfiles("MonitorControllerTest")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT, 
    classes = MonitorRunner.class, 
    properties = {
        "executor.file.path=src/test/resources/webapp-test-executors"
    }
)
public class MonitorControllerTest {
    
    @LocalServerPort
    private int port;

    @Test
    public void testEndpointWired() throws IOException {
        try (TestController controller = new TestController(22224)) {
            GeneralStatus result = HttpQuery.normalQuery("http://localhost:" + port + "/api/status", GeneralStatus.class);
            assertNotNull(result);
            assertEquals(0, result.getExecutors().size());
            assertEquals(0, result.getQueryRunners().size());
        }
    }
}