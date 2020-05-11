package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

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

import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.tracker.controller.Invocation;
import sos.accumulo.monitor.tracker.controller.TestController;

@RunWith(SpringRunner.class)
@ActiveProfiles("ProxyDaoTargetTest")
@WebMvcTest(
    controllers = {}, 
    useDefaultFilters = false, 
    properties = {
        "origin.proxy.address=localhost:43335",
        "local.server.port=12345",
        "proxy.id=test-id"
    })
public class ProxyDaoTargetTest {

    private static volatile TestController controller = null;

    @Autowired
    private ProxyDao dao;

    @Profile("ProxyDaoTargetTest")
    @Configuration
    public static class TestConfig {
        @Bean
        public ProxyDao proxy() {
            return new ProxyDaoTarget();
        }
        @Bean 
        public TrackerAddress address() {
            TrackerAddress address = mock(TrackerAddress.class);
            when(address.get()).thenReturn("127.0.0.1:12345");
            return address;
        }
    }

    @BeforeClass
    public static void setup() {
        if (controller != null) {
            controller.close();
        }
        controller = new TestController(43335);
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
    public void startQueryTest() throws IOException {
        controller.setNextReturnObject(5L);

        assertEquals(5, dao.startProxyQuery());

        assertEquals(1, controller.getInvocations().size());
        
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/proxy/start");
        invocation.assertNumParams(2);
        invocation.assertParamEquals("address", "127.0.0.1:12345");
        invocation.assertParamEquals("id", "test-id");
    }

    @Test
    public void finishQueryTest() throws IOException {
        long now = System.currentTimeMillis();
        ProxyQuery query = new ProxyQuery(mock(RunnerTracker.class), 7, new QueryInfo.Builder()
            .setIndex(7)
            .setStarted(now - 10)
            .setOriginThreadName("origin thread")
            .setQueryString("a query string")
            .setShardsComplete(99)
            .setShardsTotal(99)
            .setFinished(now));

        dao.finishProxyQuery(query);

        assertEquals(1, controller.getInvocations().size());
        
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/proxy/finished");
        invocation.assertNumParams(0);

        assertNotNull(invocation.getBody());
        QueryInfo posted = new ObjectMapper().readValue(invocation.getBody(), QueryInfoDetail.class).getInfo();

        assertEquals(7, posted.getIndex());
        assertEquals(now - 10, posted.getStarted());
        assertEquals("origin thread", posted.getOriginThreadName());
        assertEquals("a query string", posted.getQueryString());
        assertEquals(99, posted.getShardsComplete());
        assertEquals(99, posted.getShardsTotal());
    }

    @Test
    public void recordErrorTest() {
        dao.recordError("this is an error");
        
        assertEquals(1, controller.getInvocations().size());
        
        Invocation invocation = controller.getInvocations().get(0);
        
        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/proxy/error");
        invocation.assertNumParams(1);
        invocation.assertParamEquals("error", "this is an error");
    }
}