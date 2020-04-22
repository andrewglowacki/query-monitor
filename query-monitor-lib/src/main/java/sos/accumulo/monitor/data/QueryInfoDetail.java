package sos.accumulo.monitor.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryInfoDetail implements Comparable<QueryInfoDetail> {
    private final QueryInfo info;
    private final List<ShardInfo> shards;

    @JsonCreator
    private QueryInfoDetail(@JsonProperty QueryInfo info, @JsonProperty List<ShardInfo> shards) {
        this.info = info;
        this.shards = shards;
    }

    public QueryInfo getInfo() {
        return info;
    }
    public List<ShardInfo> getShards() {
        return shards;
    }

    @Override
    public int compareTo(QueryInfoDetail o) {
        return info.compareTo(o.info);
    }
    
}