package sos.accumulo.monitor.tracker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.ShardInfo;

public class ProxyQuery implements AutoCloseable {

    private final RunnerTracker tracker;
    private final long queryIndex;
    private final QueryInfoDetail detail;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ProxyQuery(RunnerTracker tracker, long queryIndex, QueryInfo.Builder builder) {
        this.tracker = tracker;
        this.queryIndex = queryIndex;
        if (queryIndex < 0) {
            this.detail = new QueryInfoDetail(builder.setIndex(tracker.nextQueryIndex()).build(), new CopyOnWriteArrayList<>());
        } else {
            this.detail = new QueryInfoDetail(builder.setIndex(queryIndex).build(), new CopyOnWriteArrayList<>());
        }
        tracker.start(detail);
    }

    public QueryInfoDetail getDetail() {
        return detail;
    }
    
    public List<ShardInfo> getShards() {
        return detail.getShards();
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            tracker.finish(detail);
            if (queryIndex >= 0) {
                tracker.finishProxyQuery(this);
            }
        }
    }

}