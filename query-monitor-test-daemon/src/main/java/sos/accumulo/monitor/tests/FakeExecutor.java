package sos.accumulo.monitor.tests;

import java.util.ArrayList;
import java.util.List;

import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.QueryPartInfo;
import sos.accumulo.monitor.tracker.ExecutorTracker;
import sos.accumulo.monitor.tracker.TrackerHandle;
import sos.accumulo.monitor.tracker.TrackerServer;

public class FakeExecutor {
    public static void run(CheckedArgs args) {
        
        // verify the port is set
        args.getRequired("port");

        try (TrackerHandle handle = TrackerServer.startExecutorTracker(args.getInt("port"))) {

            ExecutorTracker tracker = TrackerServer.getExecutorTracker();

            if (args.getBoolean("manualTest")) {

                int finished = (int)Math.floor(Math.random() * 100) + 100;
                for (int i = 0; i < finished; i++) {
                    long start = System.currentTimeMillis() - ((num(120) + 120) * 1000);
                    long end = start + (num(120) * 1000) + num(1000);
                    List<QueryPartInfo> parts = createQueryParts(start, end, num(50) + 1);

                    String shardStr = "00";
                    int part = num(25);
                    if (part < 10) {
                        shardStr += "0";
                    }
                    shardStr += part + "_" + (num(5) + 1);

                    ExecutorShardInfoDetail shard = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
                        .setStarted(start)
                        .setFinished(end)
                        .setFinishedQueueCount(num(40))
                        .setIndex(i)
                        .setQueryString("this is a short query string")
                        .setResults(num(1000L))
                        .setShard(shardStr)
                        .setSourceServer("localhost")
                        .setStartedQueueCount(num(40))
                        .build(), parts);
                    
                    tracker.start(shard);
                    tracker.finish(i);
                }

                for (int i = 0; i < 25; i++) {
                    long start = System.currentTimeMillis() - ((num(60) + 60) * 1000);
                    List<QueryPartInfo> parts = createQueryParts(start, System.currentTimeMillis(), num(20) + 1);
                    
                    String shardStr = "00";
                    int part = num(25);
                    if (part < 10) {
                        shardStr += "0";
                    }
                    shardStr += part + "_" + (num(5) + 1);

                    ExecutorShardInfoDetail shard = new ExecutorShardInfoDetail(new ExecutorShardInfo.Builder()
                        .setStarted(start)
                        .setFinished(0)
                        .setIndex(i + finished)
                        .setQueryString("this is a short query string")
                        .setResults(num(1000L))
                        .setShard(shardStr)
                        .setSourceServer("localhost")
                        .setStartedQueueCount(num(40))
                        .build(), parts);
                    
                    tracker.start(shard);
                }
                
                FakeScan.run(num(25));

            } else {
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

    public static int num(int max) {
        return (int)Math.floor(Math.random() * max);
    }
    public static long num(long max) {
        return (long)Math.floor(Math.random() * max);
    }

    private static QueryPartInfo createPart(long start, long finished) {
        long results = num(10000L);
        QueryPartInfo part = new QueryPartInfo();
        part.setStarted(start);
        part.setFinished(finished);
        part.setResults(results);
        part.setUnfilteredResults(results);
        part.setWaitTime(num(finished));
        part.setPartString("queryPart='" + num(10000000L) + "'");
        part.setChildren(new ArrayList<>(0));
        return part;
    }
    
    private static List<QueryPartInfo> createQueryParts(long start, long end, int max) {
        int createHere = num(max) + 1;
        int createLeft = max - createHere;
        List<QueryPartInfo> items = new ArrayList<>();
        long duration = end - start;
        long durationEach = duration / createHere;
        long eachHalf = durationEach / 2;
        for (int i = 0; i < createHere; i++) {
            long useEnd = start + eachHalf + num(eachHalf);
            if (i == createHere - 1) {
                useEnd = end;
            }
            QueryPartInfo part = createPart(start, useEnd);
            if (createLeft > 0 && num(100) >= 70) {
                part.setChildren(createQueryParts(start, useEnd, createLeft));
                createLeft -= part.getChildren().size();
            }
            start = useEnd;
            items.add(part);
        }
        return items;
    }
}