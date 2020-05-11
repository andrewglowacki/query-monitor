package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryType;
import sos.accumulo.monitor.tracker.controller.Invocation;
import sos.accumulo.monitor.tracker.controller.TestController;

@RunWith(SpringRunner.class)
@ActiveProfiles("ProxyDaoOriginTest")
@WebMvcTest(controllers = {}, useDefaultFilters = false)
public class ProxyDaoOriginTest {
    
    private static final String ADDRESS = "localhost:43334";

    private static volatile TestController controller = null;

    @Autowired
    private ProxyDao dao;
    
    @Profile("ProxyDaoOriginTest")
    @Configuration
    public static class TestConfig {
        @Bean
        public ProxyDao proxy() {
            return new ProxyDaoOrigin();
        }
    }
    
    @BeforeClass
    public static void setup() {
        if (controller != null) {
            controller.close();
        }
        controller = new TestController(43334);
    }

    @AfterClass
    public static void cleanup() {
        if (controller != null) {
            controller.close();
            controller = null;
        }
    }

    @Before
    public void reset() {
        controller.getInvocations().clear();
    }

    @Test
    public void noopTests() throws IOException {
        assertEquals(-1, dao.startProxyQuery());
        dao.finishProxyQuery(null);
        assertEquals(false, dao.recordError("any error"));
    }

    @Test
    public void getQueryDetailTest() throws IOException {

        QueryInfoDetail detail = new QueryInfoDetail(new QueryInfo.Builder()
            .setIndex(2)
            .setQueryString("query string")
            .setQueryType(QueryType.NORMAL)
            .setOriginThreadName("origin thread")
            .setShardsComplete(10)
            .setShardsTotal(100)
            .build(), new ArrayList<>());
        controller.setNextReturnObject(detail);

        QueryInfoDetail result = dao.getQueryDetail(ADDRESS, 2);

        assertEquals(1, controller.getInvocations().size());

        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/query/2");
        invocation.assertNumParams(0);

        assertNotNull(result);
        assertEquals(detail.getInfo().getIndex(), result.getInfo().getIndex());
        assertEquals(detail.getInfo().getQueryString(), result.getInfo().getQueryString());
        assertEquals(detail.getInfo().getOriginThreadName(), result.getInfo().getOriginThreadName());
        assertEquals(detail.getInfo().getShardsComplete(), result.getInfo().getShardsComplete());
        assertEquals(detail.getInfo().getShardsTotal(), result.getInfo().getShardsTotal());
    }

    @Test
    public void findMatchHitNoProxies() {
        QueryRunnerMatch match = new QueryRunnerMatch();
        assertEquals(match, dao.findMatch("query string", "0002_3", System.currentTimeMillis(), new ArrayList<>(), match));
    }

    @Test
    public void findMatchHitTest() {

        long now = System.currentTimeMillis();

        QueryRunnerMatch hit = new QueryRunnerMatch();
        hit.setAttemptServer("r1n01-node");
        hit.setAttemptStarted(now);
        hit.setFound(true);
        hit.setName("runner0");
        hit.setQuery(3);
        hit.setShard("0005_3");

        controller.setNextReturnObject(hit);

        QueryRunnerMatch existing = new QueryRunnerMatch();
        existing.setAttemptServer("r1n02-node");
        existing.setAttemptStarted(now - 10);
        existing.setFound(true);
        existing.setName("runner0");
        existing.setQuery(3);
        existing.setShard("0005_3");

        List<RegisteredProxy> proxies = new ArrayList<>();
        proxies.add(new RegisteredProxy(3, ADDRESS, "proxy-id"));
        QueryRunnerMatch result = dao.findMatch("a query string", "0005_3", now + 10, proxies, existing);

        assertEquals(1, controller.getInvocations().size());

        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/find");
        invocation.assertNumParams(3);
        invocation.assertParamEquals("queryString", "a query string");
        invocation.assertParamEquals("shard", "0005_3");
        invocation.assertParamEquals("started", "" + (now + 10));

        assertNotNull(result);

        assertEquals("r1n01-node", result.getAttemptServer());
        assertEquals(now, result.getAttemptStarted());
        assertEquals(3, result.getQuery());
        assertEquals("0005_3", result.getShard());
        assertEquals("runner0", result.getName());
    }

    @Test
    public void findMatchMissTest() {
        // this is kind the opposite of the previous test
        long now = System.currentTimeMillis();

        QueryRunnerMatch miss = new QueryRunnerMatch();
        miss.setAttemptServer("r1n01-node");
        miss.setAttemptStarted(now - 10);
        miss.setFound(true);
        miss.setName("runner0");
        miss.setQuery(3);
        miss.setShard("0005_3");

        controller.setNextReturnObject(miss);

        QueryRunnerMatch existing = new QueryRunnerMatch();
        existing.setAttemptServer("r1n02-node");
        existing.setAttemptStarted(now);
        existing.setFound(true);
        existing.setName("runner0");
        existing.setQuery(3);
        existing.setShard("0005_3");

        List<RegisteredProxy> proxies = new ArrayList<>();
        proxies.add(new RegisteredProxy(3, ADDRESS, "proxy-id"));
        QueryRunnerMatch result = dao.findMatch("a query string", "0005_3", now + 10, proxies, existing);

        assertEquals(1, controller.getInvocations().size());

        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/find");
        invocation.assertNumParams(3);
        invocation.assertParamEquals("queryString", "a query string");
        invocation.assertParamEquals("shard", "0005_3");
        invocation.assertParamEquals("started", "" + (now + 10));

        assertNotNull(result);

        assertEquals("r1n02-node", result.getAttemptServer());
        assertEquals(now, result.getAttemptStarted());
        assertEquals(3, result.getQuery());
        assertEquals("0005_3", result.getShard());
        assertEquals("runner0", result.getName());
    }

    @Test
    public void getRunningQueriesTest() throws IOException {

        List<QueryInfo> queries = new ArrayList<>();
        queries.add(new QueryInfo.Builder()
            .setIndex(2)
            .setQueryString("query string")
            .setQueryType(QueryType.NORMAL)
            .setOriginThreadName("origin thread")
            .setShardsComplete(10)
            .setShardsTotal(100)
            .build());
        controller.setNextReturnObject(queries);

        List<QueryInfo> results = dao.getRunningQueries(ADDRESS);

        assertEquals(1, controller.getInvocations().size());

        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/running");
        invocation.assertNumParams(0);

        assertNotNull(results);
        assertEquals(1, results.size());

        QueryInfo result = results.get(0);
        assertEquals(2, result.getIndex());
        assertEquals("query string", result.getQueryString());
        assertEquals("origin thread", result.getOriginThreadName());
        assertEquals(10, result.getShardsComplete());
        assertEquals(100, result.getShardsTotal());
    }

    @Test
    public void getRunningScansTest() throws IOException {

        List<AccumuloScanInfo> scans = new ArrayList<>();
        AccumuloScanInfo scan = new AccumuloScanInfo();
        scan.setRanges(5);
        scan.setServer("r3n15-node");
        scan.setTable("d");
        scans.add(scan);
        controller.setNextReturnObject(scans);

        List<AccumuloScanInfo> results = dao.getRunningScans(ADDRESS);

        assertEquals(1, controller.getInvocations().size());

        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/scans");
        invocation.assertNumParams(0);

        assertNotNull(results);
        assertEquals(1, results.size());

        AccumuloScanInfo result = results.get(0);
        assertEquals(5, result.getRanges());
        assertEquals("r3n15-node", result.getServer());
        assertEquals("d", result.getTable());
    }

}