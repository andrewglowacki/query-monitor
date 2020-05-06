package sos.accumulo.monitor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class RunnerHealthTest {

    @Test
    public void createTest() {
        Runtime.getRuntime().gc();

        RunnerHealth health = RunnerHealth.create(0);
        assertNotEquals(0, health.getGcCount());
        assertNotEquals(0, health.getGcTime());

        Runtime runtime = Runtime.getRuntime();
        assertEquals(runtime.maxMemory(), health.getMemoryMax());
        assertEquals(runtime.totalMemory() - runtime.freeMemory(), health.getMemoryUsed());
        assertNotEquals(0, health.getUpSince());
        assertTrue("" + health.getUpSince(), health.getUpSince() > (System.currentTimeMillis() - (1000 * 60)));
        assertEquals(0, health.getMostRecentError());
        assertNotEquals(Thread.activeCount(), health.getThreads());
    }

    @Test
    public void createWithErrorTest() {
        long lastError = System.currentTimeMillis() - (1000 * 10);
        RunnerHealth health = RunnerHealth.create(lastError);
        assertEquals(lastError, health.getMostRecentError());
    }

    @Test
    public void mapTest() throws Exception {
        RunnerHealth original = RunnerHealth.create(System.currentTimeMillis() - 5000);

        ObjectMapper mapper = new ObjectMapper();
        RunnerHealth copy = mapper.readValue(mapper.writeValueAsString(original), RunnerHealth.class);

        assertEquals(original.getGcCount(), copy.getGcCount());
        assertEquals(original.getGcTime(), copy.getGcTime());
        assertEquals(original.getMemoryMax(), copy.getMemoryMax());
        assertEquals(original.getMemoryUsed(), copy.getMemoryUsed());
        assertEquals(original.getMostRecentError(), copy.getMostRecentError());
        assertEquals(original.getThreads(), copy.getThreads());
        assertEquals(original.getUpSince(), copy.getUpSince());
    }
}