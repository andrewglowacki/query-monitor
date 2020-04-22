package sos.accumulo.monitor.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutorShardInfoDetail implements Comparable<ExecutorShardInfoDetail> {
    private final List<QueryPartInfo> queryParts;
    private final ExecutorShardInfo info;

    @JsonCreator
    public ExecutorShardInfoDetail(@JsonProperty ExecutorShardInfo info, @JsonProperty List<QueryPartInfo> queryParts) {
        this.queryParts = queryParts;
        this.info = info;
    }

    public ExecutorShardInfo getInfo() {
        return info;
    }
    public List<QueryPartInfo> getQueryParts() {
        return queryParts;
    }

    @Override
    public int compareTo(ExecutorShardInfoDetail o) {
        return info.compareTo(o.info);
    }
}