package sos.accumulo.monitor.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class QueryPartInfo {
    private final long BASE_SIZE = 8 * 8;
    private String partString = "";
    private List<QueryPartInfo> children;
    private long started;
    private long finished;
    private long results;
    private long unfilteredResults;
    private long waitTime;

    @JsonIgnore
    public long getSizeEstimate() {
        long size = BASE_SIZE + (2 * partString.length());
        for (QueryPartInfo info : children) {
            size += info.getSizeEstimate();
        }
        return size;
    }

    public String getPartString() {
        return partString;
    }

    public void setPartString(String partString) {
        this.partString = partString;
    }

    public List<QueryPartInfo> getChildren() {
        return children;
    }

    public void setChildren(List<QueryPartInfo> children) {
        this.children = children;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public long getResults() {
        return results;
    }

    public void setResults(long results) {
        this.results = results;
    }

    public long getUnfilteredResults() {
        return unfilteredResults;
    }

    public void setUnfilteredResults(long unfilteredResults) {
        this.unfilteredResults = unfilteredResults;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    
}