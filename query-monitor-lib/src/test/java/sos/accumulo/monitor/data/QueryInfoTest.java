package sos.accumulo.monitor.data;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class QueryInfoTest {
    @Test
    public void buildTest() {
        QueryInfo info = new QueryInfo.Builder()
            .setError("this is an error")
            .setStarted(2005)
            .setFinished(2010)
            .setIndex(77)
            .setNumBlobIds(3123)
            .setQueryString("this is a query string")
            .setQueryType(QueryType.BLOB_ID)
            .setResultSize(1203123414L)
            .setResults(9999)
            .setResultsType(ResultsType.BLOB)
            .setShardsComplete(88)
            .setShardsTotal(103)
            .build();

        assertEquals("this is an error", info.getError());
        assertEquals(2005, info.getStarted());
        assertEquals(2010, info.getFinished());
        assertEquals(77, info.getIndex());
        assertEquals(3123, info.getNumBlobIds());
        assertEquals(Thread.currentThread().getName(), info.getOriginThreadName());
        assertEquals("this is a query string", info.getQueryString());
        assertEquals(QueryType.BLOB_ID, info.getQueryType());
        assertEquals(1203123414L, info.getResultSize());
        assertEquals(9999, info.getResults());
        assertEquals(ResultsType.BLOB, info.getResultsType());
        assertEquals(88, info.getShardsComplete());
        assertEquals(103, info.getShardsTotal());
    }

    @Test
    public void mapTest() throws Exception {
        QueryInfo info = new QueryInfo.Builder()
            .setError("this is an error")
            .setStarted(2005)
            .setFinished(2010)
            .setIndex(77)
            .setNumBlobIds(3123)
            .setOriginThreadName("this is a thread")
            .setQueryString("this is a query string")
            .setQueryType(QueryType.BLOB_ID)
            .setResultSize(1203123414L)
            .setResults(9999)
            .setResultsType(ResultsType.BLOB)
            .setShardsComplete(88)
            .setShardsTotal(103)
            .build();

        ObjectMapper mapper = new ObjectMapper();
        QueryInfo copy = mapper.readValue(mapper.writeValueAsString(info), QueryInfo.class);

        assertEquals("this is an error", copy.getError());
        assertEquals(2005, copy.getStarted());
        assertEquals(2010, copy.getFinished());
        assertEquals(77, copy.getIndex());
        assertEquals(3123, copy.getNumBlobIds());
        assertEquals("this is a thread", copy.getOriginThreadName());
        assertEquals("this is a query string", copy.getQueryString());
        assertEquals(QueryType.BLOB_ID, copy.getQueryType());
        assertEquals(1203123414L, copy.getResultSize());
        assertEquals(9999, copy.getResults());
        assertEquals(ResultsType.BLOB, copy.getResultsType());
        assertEquals(88, copy.getShardsComplete());
        assertEquals(103, copy.getShardsTotal());
    }
}