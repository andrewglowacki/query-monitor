package sos.accumulo.monitor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.test.controller.Invocation;
import sos.accumulo.monitor.test.controller.TestController;

public class HttpQueryTest {

    private static volatile TestController controller = null;

    @BeforeClass
    public static void setup() {
        if (controller != null) {
            controller.close();
        }
        controller = new TestController(33333);
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
        controller.reset();
    }

    @Test
    public void normalPostQueryParamsTest() throws IOException {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicHeader("apple", "red"));
        params.add(new BasicHeader("bread", "pumpernickel"));

        HttpQuery.normalPostQuery("http://localhost:33333/post/url", params);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/post/url");
        invocation.assertNumParams(2);
        invocation.assertParamEquals("apple", "red");
        invocation.assertParamEquals("bread", "pumpernickel");
        assertNull(invocation.getBody());
    }

    @Test
    public void normalPostQueryBodyTest() throws IOException {
        List<String> test = Arrays.asList("apple", "bread");

        HttpQuery.normalPostQuery("http://localhost:33333/post/body/test", test);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/post/body/test");
        invocation.assertNumParams(0);

        assertNotNull(invocation.getBody());
        
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, String.class);
        List<String> posted = new ObjectMapper().readValue(invocation.getBody(), type);

        assertEquals(test, posted);
    }

    @Test
    public void normalPostQueryResponseTest() throws IOException {

        controller.setNextReturnObject(4123L);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("apple", "green"));

        long result = HttpQuery.normalPostQuery("http://localhost:33333/post/with/result", params, Long.class);
        assertEquals(4123L, result);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.POST);
        invocation.assertPathEquals("/post/with/result");
        invocation.assertNumParams(1);
        invocation.assertParamEquals("apple", "green");
        assertNull(invocation.getBody());
    }

    @Test
    public void normalQueryTest() throws IOException {
        controller.setNextReturnObject(100L);
        
        long result = HttpQuery.normalQuery("http://localhost:33333/test/query/url?apple=red&bread=rye", Long.class);
        
        assertEquals(100, result);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/test/query/url");
        invocation.assertNumParams(2);
        invocation.assertParamEquals("apple", "red");
        invocation.assertParamEquals("bread", "rye");
        assertNull(invocation.getBody());
    }

    @Test
    public void normalQueryNoParamsTest() throws IOException {
        controller.setNextReturnObject(100L);
        
        long result = HttpQuery.normalQuery("http://localhost:33333/test/query/url", Long.class);
        
        assertEquals(100, result);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/test/query/url");
        invocation.assertNumParams(0);
        assertNull(invocation.getBody());
    }
    @Test
    public void normalQueryJavaTypeTest() throws IOException {
        controller.setNextReturnObject(Arrays.asList(100L));
        
        JavaType type = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Long.class);
        List<Long> result = HttpQuery.normalQuery("http://localhost:33333/another/url?apple=blue&bread=seeded", type);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).longValue());

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/another/url");
        invocation.assertNumParams(2);
        invocation.assertParamEquals("apple", "blue");
        invocation.assertParamEquals("bread", "seeded");
        assertNull(invocation.getBody());
    }
    @Test
    public void quickQueryTest() throws IOException {
        controller.setNextReturnObject(100L);
        
        long result = HttpQuery.quickQuery("http://localhost:33333/test/query/url?apple=red&bread=rye", Long.class);
        
        assertEquals(100, result);

        assertEquals(1, controller.getInvocations().size());
        Invocation invocation = controller.getInvocations().get(0);

        invocation.assertMethodEquals(RequestMethod.GET);
        invocation.assertPathEquals("/test/query/url");
        invocation.assertNumParams(2);
        invocation.assertParamEquals("apple", "red");
        invocation.assertParamEquals("bread", "rye");
        assertNull(invocation.getBody());
    }
}