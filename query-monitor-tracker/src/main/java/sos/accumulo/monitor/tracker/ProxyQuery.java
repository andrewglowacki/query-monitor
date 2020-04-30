package sos.accumulo.monitor.tracker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.ShardInfo;

public class ProxyQuery implements AutoCloseable {

    private final long queryIndex;
    private final QueryInfoDetail detail;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ProxyQuery(long queryIndex, QueryInfo.Builder builder) {
        this.queryIndex = queryIndex;
        this.detail = new QueryInfoDetail(builder.setIndex(queryIndex).build(), new CopyOnWriteArrayList<>());
        RunnerTracker.getInstance().start(detail);
    }

    public QueryInfoDetail getDetail() {
        return detail;
    }
    
    public List<ShardInfo> getShards() {
        return detail.getShards();
    }

    @Override
    public void close() {
        if (queryIndex >= 0) {
            if (closed.getAndSet(true)) {
                RunnerTracker.getInstance().finish(detail);
                RunnerTracker.getInstance().finishProxyQuery(this);
            }
        }
    }

}