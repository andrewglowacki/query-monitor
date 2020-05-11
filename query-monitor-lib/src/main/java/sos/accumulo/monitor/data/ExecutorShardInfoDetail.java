package sos.accumulo.monitor.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;

public class ExecutorShardInfoDetail implements Comparable<ExecutorShardInfoDetail> {
    private final long BASE_SIZE_ESTIMATE = 28;
    private final List<QueryPartInfo> queryParts;
    private final ExecutorShardInfo info;

    @JsonCreator(mode = Mode.PROPERTIES)
    public ExecutorShardInfoDetail(@JsonProperty("info") ExecutorShardInfo info, @JsonProperty("queryParts") List<QueryPartInfo> queryParts) {
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

    @JsonIgnore
    public long getSizeEstimate() {
        long partsEstimate = 0;
        for (QueryPartInfo part : queryParts) {
            partsEstimate += part.getSizeEstimate();
        }
        return BASE_SIZE_ESTIMATE + partsEstimate + info.getSizeEstimate();
    }
}