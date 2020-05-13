package sos.accumulo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ExecutorFileMonitorTest {
    @Test
    public void test() throws IOException {
        File from = new File("src/test/resources/executor-test-file");
        File to = new File("target/executor-test-file");
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ExecutorFileMonitor monitor = new ExecutorFileMonitor(to.getPath());

        assertEquals(to.lastModified(), monitor.getLastLoaded());

        Set<String> executors = monitor.getExecutors();
        assertEquals(25, executors.size());

        for (int r = 1; r <= 5; r++) {
            for (int n = 1; n <= 5; n++) {
                String name = "r" + r + "n0" + n + "-node";
                assertTrue(executors.contains(name), "Expected " + name + " to exist, but didn't");
            }
        }
        
        // updating again should have no affect - the reference should be the same because no update occurred
        monitor.updateExecutors();
        assertTrue(executors == monitor.getExecutors());
    }
}