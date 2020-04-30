package sos.accumulo.monitor.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryInfoDetail implements Comparable<QueryInfoDetail> {
    private static final int BASE_SIZE_ESTIMATE = 12;
    private final QueryInfo info;
    private final List<ShardInfo> shards;

    @JsonCreator
    public QueryInfoDetail(@JsonProperty QueryInfo info, @JsonProperty List<ShardInfo> shards) {
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

    @JsonIgnore
	public long getSizeEstimate() {
        long shardSize = shards.size() * 8;
        for (ShardInfo info : shards) {
            shardSize += info.getSizeEstimate();
        }
		return BASE_SIZE_ESTIMATE + shardSize + info.getSizeEstimate();
	}
    
}