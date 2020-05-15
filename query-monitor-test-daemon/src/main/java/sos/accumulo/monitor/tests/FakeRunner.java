package sos.accumulo.monitor.tests;

import java.io.IOException;
import java.util.ArrayList;

import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.tracker.RunnerTracker;
import sos.accumulo.monitor.tracker.TrackerHandle;
import sos.accumulo.monitor.tracker.TrackerServer;

public class FakeRunner {
    public static void run(CheckedArgs args) {
        String announceAddress = args.getRequired("announce.address");
        String name = args.getRequired("name");

        try (TrackerHandle handle = TrackerServer.startRunnerTracker(announceAddress, name)) {

            RunnerTracker tracker = TrackerServer.getRunnerTracker();

            if (args.getBoolean("proxy")) {

                try {
                    try (DaemonRunner proxy = DaemonRunner.runTracker(
                        "mode=proxy", 
                        "origin.address=" + handle.getTrackerAddress(), 
                        "proxy.id=test-proxy", 
                        "scans=" + args.getInt("scans"))) {
                        
                        proxy.waitForTrackerReady();
                        
                        FakeMain.waitForInterrupt();

                        return;
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Could not run proxy", ex);
                }

            } else {
                FakeScan.run(args.getInt("scans"));

                int finished = args.getInt("finished");
                for (int i = 0; i < finished; i++) {
                    QueryInfoDetail detail = createQuery();
                    tracker.start(detail);
                    tracker.finish(detail);
                }

                int running = args.getInt("running");
                for (int i = 0; i < running; i++) {
                    tracker.start(createQuery());
                }
            }

            FakeMain.waitForInterrupt();
        }
    }

    public static QueryInfoDetail createQuery() {
        return new QueryInfoDetail(
            new QueryInfo.Builder()
                .setIndex(TrackerServer.getRunnerTracker().nextQueryIndex())
                .setQueryString("a query string")
                .setShardsTotal(100)
                .build(), 
            new ArrayList<>());
    }
}