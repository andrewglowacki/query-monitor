package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import sos.accumulo.monitor.data.QueryInfo;

public class ProxyQueryTest {
    @Test
    public void normalTest() {
        RunnerTracker tracker = mock(RunnerTracker.class);

        ProxyQuery query = new ProxyQuery(tracker, 1, new QueryInfo.Builder()
            .setQueryString("a query string")
            .setShardsTotal(100));
        
        assertEquals(1, query.getDetail().getInfo().getIndex());

        verify(tracker, times(1)).start(eq(query.getDetail()));

        query.close();

        verify(tracker, times(1)).finish(eq(query.getDetail()));
        verify(tracker, times(1)).finishProxyQuery(eq(query));
        
        query.close();

        verify(tracker, times(1)).finish(any());
        verify(tracker, times(1)).finishProxyQuery(any());
    }

    @Test
    public void noQueryIdTest() {
        RunnerTracker tracker = mock(RunnerTracker.class);

        ProxyQuery query = new ProxyQuery(tracker, -1, new QueryInfo.Builder()
            .setQueryString("a query string")
            .setShardsTotal(100));

        assertEquals(0, query.getDetail().getInfo().getIndex());
        
        verify(tracker, times(1)).start(eq(query.getDetail()));

        query.close();

        verify(tracker, times(1)).finish(eq(query.getDetail()));
        verify(tracker, times(0)).finishProxyQuery(any());

    }
}