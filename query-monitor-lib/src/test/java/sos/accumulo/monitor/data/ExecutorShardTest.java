package sos.accumulo.monitor.data;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class ExecutorShardTest {
    @Test
    public void buildTest() {
        ExecutorShardInfo info = new ExecutorShardInfo.Builder()
            .setError("this is an error")
            .setStarted(1005)
            .setFinished(1010)
            .setIndex(15)
            .setQueryString("this is a query string")
            .setResults(99999)
            .setShard("0005_13")
            .setSourceServer("n10r4-node")
            .setStartedQueueCount(5)
            .setFinishedQueueCount(7)
            .build();

        assertEquals("this is an error", info.getError());
        assertEquals(1005, info.getStarted());
        assertEquals(1010, info.getFinished());
        assertEquals(15, info.getIndex());
        assertEquals("this is a query string", info.getQueryString());
        assertEquals(99999, info.getResults());
        assertEquals("0005_13", info.getShard());
        assertEquals("n10r4-node", info.getSourceServer());
        assertEquals(5, info.getStartedQueueCount());
        assertEquals(7, info.getFinishedQueueCount());
    }

    @Test
    public void mapTest() throws Exception {
        ExecutorShardInfo info = new ExecutorShardInfo.Builder()
            .setError("this is an error")
            .setStarted(1005)
            .setFinished(1010)
            .setIndex(15)
            .setQueryString("this is a query string")
            .setResults(99999)
            .setShard("0005_13")
            .setSourceServer("n10r4-node")
            .setStartedQueueCount(5)
            .setFinishedQueueCount(7)
            .build();

        ObjectMapper mapper = new ObjectMapper();
        ExecutorShardInfo copy = mapper.readValue(mapper.writeValueAsString(info), ExecutorShardInfo.class);
        
        assertEquals("this is an error", copy.getError());
        assertEquals(1005, copy.getStarted());
        assertEquals(1010, copy.getFinished());
        assertEquals(15, copy.getIndex());
        assertEquals("this is a query string", copy.getQueryString());
        assertEquals(99999, copy.getResults());
        assertEquals("0005_13", copy.getShard());
        assertEquals("n10r4-node", copy.getSourceServer());
        assertEquals(5, copy.getStartedQueueCount());
        assertEquals(7, copy.getFinishedQueueCount());
    }
}