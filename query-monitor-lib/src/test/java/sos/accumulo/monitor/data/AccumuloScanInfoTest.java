package sos.accumulo.monitor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class AccumuloScanInfoTest {
    @Test
    public void listTest() throws InterruptedException {
        final AtomicBoolean stop = new AtomicBoolean();
        final AtomicInteger ready = new AtomicInteger();
        try {
            runThread(ready, stop, "thread-4-pool-5 looking up 25 ranges at r4n07-node");
            runThread(ready, stop, "thread-5-pool-5 looking up 44 ranges at r3n02-node");
            runThread(ready, stop, "Starting scan tserver=r5n03-node tableId=a");
            runThread(ready, stop, "Continuing scan tserver=r6n03-node scanid=12301230123");
            runThread(ready, stop, "Continuing scan tserver=r3n04-node scanid=34213412312");

            while (ready.get() < 5) {
                Thread.sleep(10);
            }

            List<AccumuloScanInfo> scans = AccumuloScanInfo.listScans();

            stop.set(true);

            assertEquals(5, scans.size());
            for (AccumuloScanInfo scan : scans) {
                switch (scan.getServer()) {
                    case "r4n07-node":
                        assertEquals(25, scan.getRanges());
                        assertNull(scan.getTable());
                        break;
                    case "r3n02-node":
                        assertEquals(44, scan.getRanges());
                        assertNull(scan.getTable());
                        break;
                    case "r5n03-node":
                        assertEquals(1, scan.getRanges());
                        assertEquals("a", scan.getTable());
                        break;
                    case "r6n03-node":
                        assertEquals(1, scan.getRanges());
                        assertNull(scan.getTable());
                        break;
                    case "r3n04-node":
                        assertEquals(1, scan.getRanges());
                        assertNull(scan.getTable());
                        break;
                    default:
                        fail("Unexpected server in scan: " + scan);
                        break;
                }
            }

        } finally {
            stop.set(true);
        }
    }

    @Test
    public void noScansTest() {
        assertTrue(AccumuloScanInfo.listScans().isEmpty());
    }

    @Test
    public void mapTest() throws Exception {
        AccumuloScanInfo info = new AccumuloScanInfo();
        info.setRanges(44);
        info.setServer("r7n11-node");
        info.setTable("z");

        ObjectMapper mapper = new ObjectMapper();
        AccumuloScanInfo copy = mapper.readValue(mapper.writeValueAsString(info), AccumuloScanInfo.class);

        assertEquals(44, copy.getRanges());
        assertEquals("r7n11-node", copy.getServer());
        assertEquals("z", copy.getTable());
    }

    private void runThread(AtomicInteger ready, AtomicBoolean stop, String name) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.incrementAndGet();
                while (!stop.get()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.setName(name);
        thread.start();
    }
}