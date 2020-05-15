package sos.accumulo.monitor.tests;

import java.util.ArrayList;

import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.tracker.ExecutorTracker;
import sos.accumulo.monitor.tracker.TrackerHandle;
import sos.accumulo.monitor.tracker.TrackerServer;

public class FakeExecutor {
    public static void run(CheckedArgs args) {
        
        // verify the port is set
        args.getRequired("port");

        try (TrackerHandle handle = TrackerServer.startExecutorTracker(args.getInt("port"))) {

            ExecutorTracker tracker = TrackerServer.getExecutorTracker();

            FakeScan.run(args.getInt("scans"));

            int finished = args.getInt("finished");
            for (int i = 0; i < finished; i++) {
                ExecutorShardInfoDetail detail = createShard(i);
                tracker.start(detail);
                tracker.finish(detail.getInfo().getIndex());
            }

            int running = args.getInt("running");
            for (int i = 0; i < running; i++) {
                tracker.start(createShard(i + finished));
            }

            FakeMain.waitForInterrupt();
        }
    }

    public static ExecutorShardInfoDetail createShard(int index) {
        return new ExecutorShardInfoDetail(
            new ExecutorShardInfo.Builder()
                .setIndex(index)
                .setShard("0005_3")
                .setSourceServer("r1n03-node")
                .setStartedQueueCount(3)
                .setQueryString("a query string")
                .build(), 
            new ArrayList<>());
    }
}