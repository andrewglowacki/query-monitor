package sos.accumulo.monitor;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.test.controller.Invocation;
import sos.accumulo.monitor.test.controller.TestController;
import sos.accumulo.monitor.util.HttpQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

@ActiveProfiles("ExecutorControllerTest")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT, 
    classes = MonitorRunner.class, 
    properties = {
        "executor.file.path=src/test/resources/localhost-executors", 
        "executor.monitor.port=22222" 
    }
)
public class ExecutorControllerTest {

    private static volatile TestController controller = null;

    @LocalServerPort
    private int port;

    @Profile("ExecutorControllerTest")
    @Configuration
    public static class TestConfig {
        @Bean
        public GeneralStatusDao generalStatusDao() {
            return new GeneralStatusDaoImpl();
        }
    }

    @BeforeAll
    public static void setup() {
        if (controller != null) {
            controller.close();
        }
        controller = new TestController(22222);
    }

    @AfterAll
    public static void cleanup() {
        if (controller != null) {
            controller.close();
            controller = null;
        }
    }

    @BeforeEach
    public void reset() {
        controller.reset();
    }

    @Test
    public void testProxyBehavior() throws Exception {
        long now = System.currentTimeMillis();

        ExecutorStatusDetail detail = new ExecutorStatusDetail();
        detail.setFinished(10);
        detail.setRunning(5);
        detail.setRecentErrorCount(7);
        detail.setLastHeard(now - 10);

        controller.setNextReturnObject("/status-detail", detail);

        ExecutorStatusDetail result = HttpQuery.normalQuery("http://localhost:" + port + "/api/executor/localhost/status", ExecutorStatusDetail.class);

        List<Invocation> invocations = controller.getInvocationsByPath("/status-detail");
        assertEquals(1, invocations.size());

        Invocation invocation = invocations.get(0);
        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/status-detail");
        invocation.assertNumParams(0);
        assertNull(invocation.getBody());

        assertEquals(10, result.getFinished());
        assertEquals(5, result.getRunning());
        assertEquals(7, result.getRecentErrorCount());
        assertEquals(now - 10, result.getLastHeard());
    }

    @Test
    public void unknownExecutorTest() {
        try {
            HttpQuery.normalQuery("http://localhost:" + port + "/api/executor/nonexistent/status", ExecutorStatusDetail.class);
            fail("Expected query to throw exception due to unknown executor: 'nonexistent'");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("No query runner or executor exists with name: nonexistent"), ex.getMessage());
        }
    }

    @Test
    public void testEndpointsWired() throws IOException {
        ExecutorStatusDetail detail = new ExecutorStatusDetail();
        detail.setFinished(10);
        detail.setRunning(5);
        detail.setRecentErrorCount(7);
        detail.setLastHeard(System.currentTimeMillis());

        ExecutorShardInfoDetail shard = new ExecutorShardInfoDetail(
                new ExecutorShardInfo.Builder()
                    .setIndex(1)
                    .setShard("0003_5")
                    .setStartedQueueCount(5)
                    .build(),
                new ArrayList<>());

        assertEndpointWorks("running", new ArrayList<ExecutorShardInfo>());
        assertEndpointWorks("finished", new ArrayList<ExecutorShardInfo>());
        assertEndpointWorks("scans", new ArrayList<AccumuloScanInfo>());
        assertEndpointWorks("shard/1", shard);
        assertEndpointWorks("status", "status-detail", detail);
        
        controller.setNextReturnObject("/find", shard);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("queryString", "a query string"));
        params.add(new BasicHeader("shard", "0003_5"));
        params.add(new BasicHeader("started", "" + System.currentTimeMillis()));
        JsonNode result = HttpQuery.normalPostQuery("http://localhost:" + port + "/api/executor/localhost/find", params, JsonNode.class);
        assertNotNull(result);
    }

    private void assertEndpointWorks(String url, Object retObject) throws IOException {
        assertEndpointWorks(url, url, retObject);
    }

    private void assertEndpointWorks(String monitorUrl, String executorUrl, Object retObject) throws IOException {
        executorUrl = "/" + executorUrl;
        controller.setNextReturnObject(executorUrl, retObject);

        JsonNode result = HttpQuery.normalQuery("http://localhost:" + port + "/api/executor/localhost/" + monitorUrl, JsonNode.class);
        assertNotNull(result);
        
        List<Invocation> invocations = controller.getInvocationsByPath(executorUrl);
        assertNotNull(invocations);

        assertEquals(1, invocations.size());

        Invocation invocation = invocations.get(0);
        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals(executorUrl);
        invocation.assertNumParams(0);
        invocation.assertNoBody();
    }
}