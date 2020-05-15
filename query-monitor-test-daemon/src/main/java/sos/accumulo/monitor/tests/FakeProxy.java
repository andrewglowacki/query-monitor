package sos.accumulo.monitor.tests;

import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.tracker.ProxyQuery;
import sos.accumulo.monitor.tracker.RunnerTracker;
import sos.accumulo.monitor.tracker.TrackerHandle;
import sos.accumulo.monitor.tracker.TrackerServer;

public class FakeProxy {
    public static void run(CheckedArgs args) {

        try (TrackerHandle handle = TrackerServer.startRunnerProxy(args.getRequired("origin.address"), args.getRequired("proxy.id"))) {

            RunnerTracker tracker = TrackerServer.getRunnerTracker();

            FakeScan.run(args.getInt("scans"));

            try (ProxyQuery query = tracker.startProxyQuery(new QueryInfo.Builder()
                .setQueryString("a proxy query string!")
                .setShardsTotal(100))) {

                FakeMain.waitForInterrupt();
            }
        }
    }
}