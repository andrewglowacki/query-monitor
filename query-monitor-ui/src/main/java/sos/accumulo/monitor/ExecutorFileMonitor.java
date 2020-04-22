package sos.accumulo.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorFileMonitor implements Runnable {

    private final Logger log = LoggerFactory.getLogger(ExecutorFileMonitor.class);
    private final File executorFile;
    private volatile long lastLoaded = 0;
    private volatile Set<String> executors = null;

    public ExecutorFileMonitor(String executorFilePath) {
        this.executorFile = new File(executorFilePath);
        if (!this.executorFile.isFile()) {
            throw new RuntimeException("Executor file does not exist: " + executorFile);
        }
        try {
            updateExecutors();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to populate initial executors set.", ex);
        }
    }

    public Set<String> getExecutors() {
        return executors;
    }

    protected void updateExecutors() throws IOException {
        long lastModified = executorFile.lastModified();
        if (lastLoaded == 0 || lastModified > lastLoaded) {
            try (BufferedReader reader = new BufferedReader(new FileReader(executorFile))) {
                executors = new TreeSet<>(reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList()));
            }
            lastLoaded = lastModified;
        }
    }

    @Override
    public void run() {
        try {
            updateExecutors();
        } catch(Throwable ex) {
            log.error("Failed to check executor file for updates.", ex);
        }
    }

}