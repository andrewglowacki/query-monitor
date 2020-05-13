package sos.accumulo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

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
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.test.controller.Invocation;
import sos.accumulo.monitor.test.controller.TestController;
import sos.accumulo.monitor.util.HttpQuery;

@ActiveProfiles("QueryRunnerControllerTest")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT, 
    classes = MonitorRunner.class, 
    properties = {
        "executor.file.path=src/test/resources/webapp-test-executors"
    }
)
public class QueryRunnerControllerTest {
    
    private static volatile TestController controller = null;

    @LocalServerPort
    private int port;

    @Profile("QueryRunnerControllerTest")
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
        controller = new TestController(22223);
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

        QueryRunnerStatus status = new QueryRunnerStatus();
        status.setFinished(10);
        status.setRunning(5);
        status.setLastHeard(now - 10);

        registerRunner();

        controller.setNextReturnObject("/status", status);

        QueryRunnerStatus result = HttpQuery.normalQuery("http://localhost:" + port + "/api/runner/test-runner/status", QueryRunnerStatus.class);

        List<Invocation> invocations = controller.getInvocationsByPath("/status");
        assertEquals(1, invocations.size());

        Invocation invocation = invocations.get(0);
        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/status");
        invocation.assertNumParams(0);
        assertNull(invocation.getBody());

        assertEquals(10, result.getFinished());
        assertEquals(5, result.getRunning());
        assertEquals(now - 10, result.getLastHeard());
    }

    @Test
    public void testEndpointsWired() throws IOException {
        registerRunner();

        QueryRunnerStatus status = new QueryRunnerStatus();
        status.setFinished(10);
        status.setRunning(5);
        status.setLastHeard(System.currentTimeMillis());

        QueryInfoDetail query = new QueryInfoDetail(
                new QueryInfo.Builder()
                    .setIndex(1)
                    .setQueryString("a query string")
                    .setStarted(System.currentTimeMillis())
                    .setShardsTotal(100)
                    .build(),
                new ArrayList<>());

        assertEndpointWorks("running", new ArrayList<QueryInfo>());
        assertEndpointWorks("finished", new ArrayList<QueryInfo>());
        assertEndpointWorks("scans", new ArrayList<AccumuloScanInfo>());
        assertEndpointWorks("query/1", query);
        assertEndpointWorks("status", status);
        
        controller.setNextReturnObject("/find", query);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("server", "test-runner"));
        params.add(new BasicHeader("queryString", "a query string"));
        params.add(new BasicHeader("shard", "0003_5"));
        params.add(new BasicHeader("started", "" + System.currentTimeMillis()));
        JsonNode result = HttpQuery.normalPostQuery("http://localhost:" + port + "/api/runner/find", params, JsonNode.class);
        assertNotNull(result);
    }

    private void registerRunner() throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("name", "test-runner"));
        params.add(new BasicHeader("address", "localhost:22223"));

        HttpQuery.normalPostQuery("http://localhost:" + port + "/api/runner/register", params);
    }

    private void assertEndpointWorks(String url, Object retObject) throws IOException {
        assertEndpointWorks(url, url, retObject);
    }

    private void assertEndpointWorks(String monitorUrl, String runnerUrl, Object retObject) throws IOException {
        runnerUrl = "/" + runnerUrl;
        controller.setNextReturnObject(runnerUrl, retObject);

        JsonNode result = HttpQuery.normalQuery("http://localhost:" + port + "/api/runner/test-runner/" + monitorUrl, JsonNode.class);
        assertNotNull(result);

        List<Invocation> invocations = controller.getInvocationsByPath(runnerUrl);
        assertNotNull(invocations);

        assertEquals(1, invocations.size());

        Invocation invocation = invocations.get(0);
        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals(runnerUrl);
        invocation.assertNumParams(0);
        invocation.assertNoBody();
    }
}