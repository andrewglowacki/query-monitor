package sos.accumulo.monitor.tests;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FakeMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        Map<String, String> argMap;
        try {
            argMap = Arrays.stream(args).map(arg -> arg.split("=", 2))
                    .collect(Collectors.toMap(arg -> arg[0], arg -> arg[1]));
        } catch (Exception ex) {
            System.err.println("Error parsing arguments: " + ex.getMessage());
            printUsage();
            return;
        }

        String mode = argMap.remove("mode");
        if (mode == null) {
            System.out.println("Error: 'mode' argument not provided.");
            printUsage();
            return;
        }

        CheckedArgs checkedArgs = new CheckedArgs(argMap);
        switch (mode.toLowerCase()) {
            case "executor":
                FakeExecutor.run(checkedArgs);
                break;
            case "runner":
                FakeRunner.run(checkedArgs);
                break;
            case "proxy":
                FakeProxy.run(checkedArgs);
                break;
            default:
                System.out.println("Error: 'mode' is invalid. Must be one of: 'executor', 'runner', or 'proxy'");
                printUsage();
                return;
        }
    }

    protected static void waitForInterrupt() {
        System.out.println("Fake Tracker Ready - test data populated");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
            return;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar query-monitor-test-daemon.jar mode=<executor|runner> (arg1=value1 arg2=value2 ...)");
        System.out.println("Available arguments are:");
        System.out.println("  mode: whether to run the 'executor' or the (query) 'runner'");
        System.out.println("  running: the number of running queries/shards to add");
        System.out.println("  finished: the number of finished queries/shards to add");
        System.out.println("  scans: the number of scans to 'run'");
        System.out.println("  proxy-queries: the number of proxy queries to run");
        System.out.println("NOTE: 'mode' is required");
    }
}