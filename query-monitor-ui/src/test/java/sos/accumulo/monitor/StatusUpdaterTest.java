package sos.accumulo.monitor;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.data.RunnerHealth;
import sos.accumulo.monitor.test.controller.Invocation;
import sos.accumulo.monitor.test.controller.TestController;

public class StatusUpdaterTest {
    @Test
    public void fullUpdateTest() {
        ExecutorFileMonitor monitor = mock(ExecutorFileMonitor.class);
        when(monitor.getExecutors()).thenReturn(new TreeSet<>(Arrays.asList("r1n01-node", "r1n02-node")));

        Map<String, String> runners = new HashMap<>();
        runners.put("runner1", "address1");
        runners.put("runner2", "address2");

        StatusUpdater updater = spy(new StatusUpdater(monitor, runners, 54321) {
            @Override
            protected void updateExecutor(String name) { }
            @Override
            protected void updateQueryRunner(String name, String address) { }
        });
        when(updater.shouldUpdate(anyString())).thenReturn(true);

        updater.run();

        verify(updater, times(1)).shouldUpdate(eq("runner1"));
        verify(updater, times(1)).shouldUpdate(eq("runner2"));
        verify(updater, times(1)).shouldUpdate(eq("r1n01-node"));
        verify(updater, times(1)).shouldUpdate(eq("r1n02-node"));
        verify(updater, times(4)).shouldUpdate(anyString());

        verify(updater, times(1)).updateExecutor(eq("r1n01-node"));
        verify(updater, times(1)).updateExecutor(eq("r1n02-node"));
        verify(updater, times(2)).updateExecutor(anyString());

        verify(updater, times(1)).updateQueryRunner(eq("runner1"), eq("address1"));
        verify(updater, times(1)).updateQueryRunner(eq("runner2"), eq("address2"));
        verify(updater, times(2)).updateQueryRunner(anyString(), anyString());

        verify(monitor, times(2)).getExecutors();
    }
    @Test
    public void partialUpdateTest() {
        ExecutorFileMonitor monitor = mock(ExecutorFileMonitor.class);
        when(monitor.getExecutors()).thenReturn(new TreeSet<>(Arrays.asList("r1n01-node", "r1n02-node")));

        Map<String, String> runners = new TreeMap<>();
        runners.put("runner1", "address1");
        runners.put("runner2", "address2");

        StatusUpdater updater = spy(new StatusUpdater(monitor, runners, 54321) {
            @Override
            protected void updateExecutor(String name) { }
            @Override
            protected void updateQueryRunner(String name, String address) { }
        });

        when(updater.shouldUpdate(anyString())).thenReturn(true, false, true, false);

        updater.run();

        verify(updater, times(1)).shouldUpdate(eq("runner1"));
        verify(updater, times(1)).shouldUpdate(eq("runner2"));
        verify(updater, times(1)).shouldUpdate(eq("r1n01-node"));
        verify(updater, times(1)).shouldUpdate(eq("r1n02-node"));
        verify(updater, times(4)).shouldUpdate(anyString());

        verify(updater, times(1)).updateExecutor(eq("r1n01-node"));
        verify(updater, times(0)).updateExecutor(eq("r1n02-node"));
        verify(updater, times(1)).updateExecutor(anyString());

        verify(updater, times(1)).updateQueryRunner(eq("runner1"), eq("address1"));
        verify(updater, times(0)).updateQueryRunner(eq("runner2"), eq("address2"));
        verify(updater, times(1)).updateQueryRunner(anyString(), anyString());

        verify(monitor, times(2)).getExecutors();
    }
    @Test
    public void generalStatusAndUpdateNowTest() {
        StatusUpdater updater = new StatusUpdater(mock(ExecutorFileMonitor.class), new TreeMap<>(), 54321);

        QueryRunnerStatus runner1 = new QueryRunnerStatus();
        QueryRunnerStatus runner2 = new QueryRunnerStatus();

        ExecutorStatusDetail executor1 = new ExecutorStatusDetail();
        executor1.setHealth(new RunnerHealth());
        executor1.setRecentErrors(new TreeSet<>());

        ExecutorStatusDetail executor2 = new ExecutorStatusDetail();
        executor2.setHealth(new RunnerHealth());
        executor2.setRecentErrors(new TreeSet<>());

        updater.updateNow("runner1", runner1);
        updater.updateNow("runner2", runner2);

        updater.updateNow("executor1", executor1);
        updater.updateNow("executor2", executor2);

        GeneralStatus status = updater.getGeneralStatus();

        assertEquals(2, status.getQueryRunners().size());
        assertEquals(runner1, status.getQueryRunners().get("runner1"));
        assertEquals(runner2, status.getQueryRunners().get("runner2"));

        assertEquals(2, status.getExecutors().size());
        assertEquals(executor1.getHealth(), status.getExecutors().get("executor1").getHealth());
        assertEquals(executor2.getHealth(), status.getExecutors().get("executor2").getHealth());
    }
    @Test
    public void shouldUpdateTest() {
        StatusUpdater updater = new StatusUpdater(mock(ExecutorFileMonitor.class), new TreeMap<>(), 54321);

        ExecutorStatusDetail executor1 = new ExecutorStatusDetail();
        executor1.setHealth(new RunnerHealth());
        executor1.setRecentErrors(new TreeSet<>());

        updater.updateNow("runner1", new QueryRunnerStatus());
        updater.updateNow("executor1", executor1);

        assertFalse(updater.shouldUpdate("runner1"));
        assertFalse(updater.shouldUpdate("executor1"));
    }
    @Test
    public void shouldUpdateTwiceTest() {
        StatusUpdater updater = new StatusUpdater(mock(ExecutorFileMonitor.class), new TreeMap<>(), 54321);

        assertTrue(updater.shouldUpdate("runner1"));
        assertTrue(updater.shouldUpdate("executor1"));

        assertFalse(updater.shouldUpdate("runner1"));
        assertFalse(updater.shouldUpdate("executor1"));
    }
    @Test
    public void updateRunnerTest() {
        long now = System.currentTimeMillis();
        try (TestController controller = new TestController(54323)) {
            StatusUpdater updater = new StatusUpdater(mock(ExecutorFileMonitor.class), new TreeMap<>(), 54323);

            QueryRunnerStatus status = new QueryRunnerStatus();
            status.setFinished(10);
            status.setLastHeard(now - 10);
            status.setRunning(5);

            controller.setNextReturnObject(status);

            updater.updateQueryRunner("runner1", "localhost:54323");

            assertEquals(1, controller.getInvocations().size());

            Invocation invocation = controller.getInvocations().get(0);
            invocation.assertMethodEquals(RequestMethod.GET);
            invocation.assertPathEquals("/status");
            invocation.assertNumParams(0);
            assertNull(invocation.getBody());

            GeneralStatus generalStatus = updater.getGeneralStatus();

            assertEquals(0, generalStatus.getExecutors().size());
            assertEquals(1, generalStatus.getQueryRunners().size());

            QueryRunnerStatus result = generalStatus.getQueryRunners().get("runner1");
            
            assertNotNull(result);
            assertEquals(10, result.getFinished());
            assertEquals(now - 10, result.getLastHeard());
            assertEquals(5, result.getRunning());
        }
    }
    @Test
    public void updateExecutorTest() {
        long now = System.currentTimeMillis();
        try (TestController controller = new TestController(54322)) {
            StatusUpdater updater = new StatusUpdater(mock(ExecutorFileMonitor.class), new TreeMap<>(), 54322);

            ExecutorStatus status = new ExecutorStatus();
            status.setFinished(10);
            status.setLastHeard(now - 10);
            status.setRunning(5);
            status.setRecentErrorCount(7);

            controller.setNextReturnObject(status);

            updater.updateExecutor("localhost");

            assertEquals(1, controller.getInvocations().size());

            Invocation invocation = controller.getInvocations().get(0);
            invocation.assertMethodEquals(RequestMethod.GET);
            invocation.assertPathEquals("/status");
            invocation.assertNumParams(0);
            assertNull(invocation.getBody());

            GeneralStatus generalStatus = updater.getGeneralStatus();

            assertEquals(1, generalStatus.getExecutors().size());
            assertEquals(0, generalStatus.getQueryRunners().size());

            ExecutorStatus result = generalStatus.getExecutors().get("localhost");
            
            assertNotNull(result);
            assertEquals(10, result.getFinished());
            assertEquals(now - 10, result.getLastHeard());
            assertEquals(5, result.getRunning());
            assertEquals(7, result.getRecentErrorCount());
        }
    }
}