package sos.accumulo.monitor.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ExecutorShardInfo.Builder.class)
public class ExecutorShardInfo implements Comparable<ExecutorShardInfo> {
    private final long index;
    private final String shard;
    private final long started;
    private final String queryString;
    private final String sourceServer;
    private final int startedQueueCount;
    private volatile long finished;
    private volatile int finishedQueueCount;
    private volatile long results;
    private volatile String error;

    @JsonPOJOBuilder(withPrefix = "set")
    public static class Builder {
        private long index;
        private String shard;
        private long started;
        private String queryString;
        private String sourceServer;
        private int startedQueueCount;
        private long finished;
        private int finishedQueueCount;
        private long results;
        private String error;

        public Builder setIndex(long index) {
            this.index = index;
            return this;
        }

        public Builder setShard(String shard) {
            this.shard = shard;
            return this;
        }

        public Builder setStarted(long started) {
            this.started = started;
            return this;
        }

        public Builder setQueryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder setSourceServer(String sourceServer) {
            this.sourceServer = sourceServer;
            return this;
        }

        public Builder setStartedQueueCount(int startedQueueCount) {
            this.startedQueueCount = startedQueueCount;
            return this;
        }

        public Builder setFinished(long finished) {
            this.finished = finished;
            return this;
        }

        public Builder setFinishedQueueCount(int finishedQueueCount) {
            this.finishedQueueCount = finishedQueueCount;
            return this;
        }

        public Builder setResults(long results) {
            this.results = results;
            return this;
        }

        public Builder setError(String error) {
            this.error = error;
            return this;
        }
        
        public ExecutorShardInfo build() {
            return new ExecutorShardInfo(this);
        }
    }

    private ExecutorShardInfo(Builder builder) {
        this.index = builder.index;
        this.shard = builder.shard;
        this.started = builder.started;
        this.queryString = builder.queryString;
        this.sourceServer = builder.sourceServer;
        this.startedQueueCount = builder.startedQueueCount;
        this.finished = builder.finished;
        this.finishedQueueCount = builder.finishedQueueCount;
        this.results = builder.results;
        this.error = builder.error;
    }

    public long getIndex() {
        return index;
    }
    
    public String getShard() {
        return shard;
    }

    public long getStarted() {
        return started;
    }

    public long getFinished() {
        return finished;
    }

    public ExecutorShardInfo setFinished(long ended) {
        this.finished = ended;
        return this;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    public int getStartedQueueCount() {
        return startedQueueCount;
    }

    public int getFinishedQueueCount() {
        return finishedQueueCount;
    }

    public ExecutorShardInfo setFinishedQueueCount(int finishedQueueCount) {
        this.finishedQueueCount = finishedQueueCount;
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public long getResults() {
        return results;
    }

    public ExecutorShardInfo setResults(long results) {
        this.results = results;
        return this;
    }

    public String getError() {
        return error;
    }

    public ExecutorShardInfo setError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public int compareTo(ExecutorShardInfo o) {
        int diff = Long.compare(started, o.started);
        if (diff != 0) {
            return diff;
        }

        diff = Long.compare(finished, o.finished);
        if (diff != 0) {
            return diff;
        }

        return Long.compare(index, o.index);
    }
}