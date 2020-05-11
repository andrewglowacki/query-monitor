package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.ArrayList;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.tracker.controller.Invocation;
import sos.accumulo.monitor.tracker.controller.TestController;
import sos.accumulo.monitor.util.HttpQuery;

public class TrackerServerTest {
    @Test
    public void proxyTest() {
        try (TrackerHandle handle = TrackerServer.startRunnerProxy("localhost:43337", "test-id")) {
            assertEquals("0", System.getProperty("server.port"));
            assertEquals("127.0.0.1", System.getProperty("server.address"));
            assertEquals("test-id", System.getProperty("proxy.id"));
            assertEquals("localhost:43337", System.getProperty("origin.proxy.address"));
            assertEquals("TrackerModeProxy", System.getProperty("spring.profiles.active"));

            assertAddress("127.0.0.1", handle);

            assertGetExecutorTrackerFails();

            assertNotNull(TrackerServer.getRunnerTracker());
        }
    }

    private void assertGetExecutorTrackerFails() {
        try {
            TrackerServer.getExecutorTracker();
            fail("Expected NoSuchBeanDefinitionException to have been thrown.");
        } catch (NoSuchBeanDefinitionException ex) {
            // expected
        }
    }

    private void assertAddress(String address, TrackerHandle handle) {
        String[] pieces = handle.getTrackerAddress().split(":");
        assertEquals(address, pieces[0]);
        int port = Integer.parseInt(pieces[1]);
        assertTrue(port > 1000);
    }

    @Test
    public void runnerTest() throws Exception {
        try (TestController controller = new TestController(43336)) {
            try (TrackerHandle handle = TrackerServer.startRunnerTracker("localhost:43336", "test-runner")) {
                assertEquals("0", System.getProperty("server.port"));
                assertEquals("localhost:43336", System.getProperty("announce.address"));
                assertEquals("test-runner", System.getProperty("runner.name"));
                assertEquals("TrackerModeRunner", System.getProperty("spring.profiles.active"));

                assertAddress(InetAddress.getLocalHost().getHostAddress(), handle);

                assertGetExecutorTrackerFails();
    
                assertNotNull(TrackerServer.getRunnerTracker());

                for (int i = 0; i < 50; i++) {
                    if (controller.getInvocations().size() == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    assertEquals(1, controller.getInvocations().size());

                    Invocation invocation = controller.getInvocations().get(0);
                    invocation.assertMethodEquals(RequestMethod.POST);
                    invocation.assertPathEquals("/api/runner/register");
                    invocation.assertNumParams(2);
                    invocation.assertParamEquals("name", "test-runner");
                    invocation.assertParamEquals("address", handle.getTrackerAddress());
                    break;
                }
            }
        }
    }
    
    @Test
    public void executorTest() throws Exception {
        try (TrackerHandle handle = TrackerServer.startExecutorTracker(43337)) {
            assertEquals("43337", System.getProperty("server.port"));
            assertEquals("TrackerModeExecutor", System.getProperty("spring.profiles.active"));
            assertEquals(InetAddress.getLocalHost().getHostAddress() + ":43337", handle.getTrackerAddress());

            try {
                TrackerServer.getRunnerTracker();
                fail("Expected NoSuchBeanDefinitionException to have been thrown.");
            } catch (NoSuchBeanDefinitionException ex) {
                // expected
            }

            assertNotNull(TrackerServer.getExecutorTracker());
        }
    }

    @Test
    public void doubleSameRunTest() throws Exception {
        try (TrackerHandle handle1 = TrackerServer.startExecutorTracker(43337)) {
            assertEquals(1, TrackerServer.getActiveCount());
            try (TrackerHandle handle2 = TrackerServer.startExecutorTracker(33334)) {
                assertEquals(handle2.getTrackerAddress(), handle1.getTrackerAddress());
                assertEquals(2, TrackerServer.getActiveCount());
            }
            assertEquals(1, TrackerServer.getActiveCount());
        }
    }

    @Test
    public void doubleDifferentRunTest() throws Exception {
        try (TrackerHandle handle1 = TrackerServer.startExecutorTracker(43337)) {
            assertEquals(1, TrackerServer.getActiveCount());
            try {
                TrackerServer.startRunnerProxy("localhost:43337", "test-id");
            } catch (IllegalArgumentException ex) {
                assertEquals("Tracker server is already running in mode: Executor", ex.getMessage());
            }
            assertEquals(1, TrackerServer.getActiveCount());
        }
    }
    
    @Test
    public void startServerTest() throws Exception {
        // verify a responsive executor server is actually running
        try (TrackerHandle handle = TrackerServer.startExecutorTracker(0)) {
            ExecutorTracker tracker = TrackerServer.getExecutorTracker();

            long now = System.currentTimeMillis();

            tracker.start(new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
                .setIndex(1)
                .setQueryString("a query string")
                .setShard("0008_12")
                .setSourceServer("some-server")
                .setStarted(now - 10)
                .setStartedQueueCount(5)
                .build(), new ArrayList<>()));

            ExecutorShardInfo info = HttpQuery.normalQuery("http://" + handle.getTrackerAddress() + "/shard/1", ExecutorShardInfoDetail.class).getInfo();

            assertNotNull(info);

            assertEquals(1, info.getIndex());
            assertEquals("a query string", info.getQueryString());
            assertEquals("0008_12", info.getShard());
            assertEquals("some-server", info.getSourceServer());
            assertEquals(now - 10, info.getStarted());
            assertEquals(5, info.getStartedQueueCount());
        }
    }
}