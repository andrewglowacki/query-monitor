package sos.accumulo.monitor.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import sos.accumulo.monitor.data.AttemptInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryType;
import sos.accumulo.monitor.data.ResultsType;
import sos.accumulo.monitor.data.ShardInfo;
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

            } else if (args.getBoolean("manualTest")) {

                long now = Long.parseLong(args.getRequired("manualStart"));
                
                List<ShardInfo> finishedShards = new ArrayList<>();

                long resultSize = (long)Math.floor(Math.random() * 10) * 1024;
                long numResults = (long)Math.floor(Math.random() * 10000.0);

                Function<Integer, ShardInfo> createShard = new Function<Integer,ShardInfo>(){
                    @Override
                    public ShardInfo apply(Integer shardNum) {
                        ShardInfo shard = new ShardInfo();
                        int num = shardNum % 25;
                        int piece = (int)Math.floor((double)shardNum / 25.0);
                        String shardStr = (num < 10 ? "0" : "") + num;
                        shard.setShard("00" + shardStr + "_" + piece);
    
                        AttemptInfo attempt = new AttemptInfo();
                        attempt.setServer("localhost");
                        attempt.setStarted(now - (1000 * 60));
                        attempt.setIndexFinished(attempt.getStarted() + 10000);
                        attempt.setIndexResults((long)Math.floor(Math.random() * 1000));
                        attempt.setFinished(attempt.getStarted() + (long)Math.floor(Math.random() * 1000 * 50) + 10000);
                        attempt.setDataFinished(attempt.getFinished());
                        attempt.setDataResults(attempt.getIndexFinished());
                        attempt.setDataSize(attempt.getIndexResults() * resultSize);
                        shard.setLatestAttempt(attempt);
                        return shard;
                    }
                };

                for (int i = 0; i < 250; i++) {
                    finishedShards.add(createShard.apply(i));
                }

                QueryInfoDetail finished = new QueryInfoDetail(
                    new QueryInfo.Builder()
                        .setIndex(tracker.nextQueryIndex())
                        .setFinished(now - (1000 * 60))
                        .setQueryString("this is a short query string")
                        .setQueryType(QueryType.NORMAL)
                        .setResultsType(ResultsType.METADATA)
                        .setResults(numResults)
                        .setResultSize(resultSize * numResults)
                        .setStarted(now - (1000 * 120))
                        .setShardsTotal(250)
                        .setShardsComplete(250)
                        .build(), finishedShards);

                tracker.start(finished);
                tracker.finish(finished);

                
                List<ShardInfo> runningShards = new ArrayList<>();

                int runningComplete = (int)Math.floor(Math.random() * 125.0) + 25;
                for (int i = 0 ; i < runningComplete; i++) {
                    runningShards.add(createShard.apply(i));
                }
                for (int i = 0; i < 25; i++) {
                    ShardInfo shard = createShard.apply(runningComplete + i);
                    shard.getLatestAttempt().setFinished(0);
                    shard.getLatestAttempt().setDataFinished(0);
                    shard.getLatestAttempt().setDataResults(0);
                    shard.getLatestAttempt().setDataSize(0);
                    shard.getLatestAttempt().setIndexFinished(0);
                    shard.getLatestAttempt().setIndexResults(0);
                    runningShards.add(shard);
                }

                QueryInfoDetail running = new QueryInfoDetail(
                    new QueryInfo.Builder()
                        .setIndex(tracker.nextQueryIndex())
                        .setQueryString("this is a short query string")
                        .setQueryType(QueryType.NORMAL)
                        .setResultsType(ResultsType.METADATA)
                        .setResults(numResults)
                        .setResultSize(resultSize * numResults)
                        .setStarted(now - (1000 * 60))
                        .setShardsTotal(250)
                        .setShardsComplete(runningComplete)
                        .build(), runningShards);
                
                tracker.start(running);

                FakeScan.run((int)Math.floor(Math.random() * 20));

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