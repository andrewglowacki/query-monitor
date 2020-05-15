package sos.accumulo.monitor.tests;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonRunner implements Closeable {

    private static final AtomicInteger counter = new AtomicInteger();
    private final int index = counter.getAndIncrement();
    private final LogTailer stderrLog;
    private final LogTailer stdoutLog;
    private final Process process;
    private final ExecutorService logThreads = Executors.newFixedThreadPool(2);
    private final String name;
    private static volatile boolean debug = false;

    public static void setDebug(boolean debug) {
        DaemonRunner.debug = debug;
    }

    public DaemonRunner(String name, Process process) {
        this.name = name;
        Logger stderr = LoggerFactory.getLogger("Daemon-" + index + "-StdErr-" + name);
        Logger stdout = LoggerFactory.getLogger("Daemon-" + index + "-StdOut-" + name);
        stderrLog = new LogTailer(stderr, process.getErrorStream());
        stdoutLog = new LogTailer(stdout, process.getInputStream());
        this.process = process;
        logThreads.submit(stderrLog);
        logThreads.submit(stdoutLog);
        if (debug) {
            stdout.info("*** DEBUG PORT IS " + (8000 + index) + " ***");
        }
    }

    public void waitForWebReady() {
        waitForReady(LogTailer::isWebReady);
    }
    public void waitForTrackerReady() {
        waitForReady(LogTailer::isTrackerReady);
    }
    private void waitForReady(Function<LogTailer, Boolean> isReady) {
        for (int i = 0; i < 150; i++) {
            if (isReady.apply(stderrLog)) {
                return;
            } else if (isReady.apply(stdoutLog)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                return;
            }
        }
        throw new RuntimeException("Daemon (" + name + ") did not start in time - gave up");
    }
    
    public static DaemonRunner runMonitor(String executorFile, int port) throws IOException {
        return run("ui", "query-monitor-ui", executorFile, port);
    }

    public static DaemonRunner runTracker(String... args) throws IOException {
        return run("tracker", "query-monitor-test-daemon", null, -1, args);
    }

    private static DaemonRunner run(String name, String jarName, String executorFile, int port, String... args) throws IOException {
        File jarPath = new File(".").getAbsoluteFile();
        jarPath = jarPath.getParentFile().getParentFile();
        jarPath = new File(jarPath, jarName);
        jarPath = new File(jarPath, "target");
        if (!jarPath.isDirectory()) {
            throw new RuntimeException("daemon jar has not be built: " + jarName + " in: " + jarPath.getAbsolutePath());
        }
        for (File file : jarPath.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar") && file.getName().startsWith(jarName)) {
                jarPath = file;
                break;
            }
        }
        if (!jarPath.getName().endsWith(".jar")) {
            throw new RuntimeException("daemon jar has not be built: " + jarName + " in: " + jarPath.getAbsolutePath());
        }

        List<String> argList = new ArrayList<>();
        argList.add("java");
        if (executorFile != null) {
            argList.add("-D" + "executor.file.path=" + executorFile);
        }
        if (port >= 0) {
            argList.add("-D" + "server.port=" + port);
        }
        if (debug) {
            int debugPort = 8000 + counter.get();
            argList.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + debugPort);
        }
        argList.add("-jar");
        argList.add(jarPath.getAbsolutePath());
        if (args != null) {
            for (String arg : args) {
                argList.add(arg);
            }
        }

        ProcessBuilder builder = new ProcessBuilder(argList);
        return new DaemonRunner(name, builder.start());
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void close() {
        try {
            process.destroy();
            for (int i = 0; i < 20 && process.isAlive(); i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    return ;
                }
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (Throwable ex) { }

        logThreads.shutdownNow();
    }
}