package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TrackerHandleTest {
    @Test
    public void test() {
        assertEquals(0, TrackerServer.getActiveCount());

        TrackerHandle handle = new TrackerHandle("test-address");

        assertEquals(1, TrackerServer.getActiveCount());
        assertEquals("test-address", handle.getTrackerAddress());

        handle.close();
        
        assertEquals(0, TrackerServer.getActiveCount());

        handle.close();
        
        assertEquals(0, TrackerServer.getActiveCount());
    }
}