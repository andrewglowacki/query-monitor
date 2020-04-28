package sos.accumulo.monitor.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = QueryInfo.Builder.class)
public class QueryInfo implements Comparable<QueryInfo> {
    private static final long BASE_SIZE_ESTIMATE = (8 * 13) + 8;
    private final long index;
    private final long started;
    private final QueryType queryType;
    private final ResultsType resultsType;
    private final String queryString;
    private final int numBlobIds;
    private final int shardsTotal;
    private final int shardsComplete;
    private final String originThreadName;
    private final long finished;
    private final long resultSize;
    private final long results;
    private final String error;
    
    @JsonPOJOBuilder(withPrefix = "set")
    public static class Builder {
        protected long index = -1;
        protected long started = System.currentTimeMillis();
        protected QueryType queryType;
        protected ResultsType resultsType;
        protected String queryString;
        protected int numBlobIds;
        protected int shardsTotal;
        protected int shardsComplete;
        protected String originThreadName = Thread.currentThread().getName();
        protected long finished;
        protected long resultSize;
        protected long results;
        protected String error;

        public Builder setIndex(long index) {
            this.index = index;
            return this;
        }

        public Builder setStarted(long started) {
            this.started = started;
            return this;
        }

        public Builder setQueryType(QueryType queryType) {
            this.queryType = queryType;
            return this;
        }

        public Builder setResultsType(ResultsType resultsType) {
            this.resultsType = resultsType;
            return this;
        }

        public Builder setQueryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder setNumBlobIds(int numBlobIds) {
            this.numBlobIds = numBlobIds;
            return this;
        }

        public Builder setShardsTotal(int shardsTotal) {
            this.shardsTotal = shardsTotal;
            return this;
        }

        public Builder setShardsComplete(int shardsComplete) {
            this.shardsComplete = shardsComplete;
            return this;
        }

        public Builder setOriginThreadName(String originThreadName) {
            this.originThreadName = originThreadName;
            return this;
        }

        public Builder setFinished(long finished) {
            this.finished = finished;
            return this;
        }

        public Builder setResultSize(long resultSize) {
            this.resultSize = resultSize;
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
        
        public QueryInfo build() {
            if (index == -1) {
                throw new IllegalStateException("index field is not set.");
            }
            if (queryString == null && numBlobIds <= 0) {
                throw new IllegalStateException("neither queryString nor numBlobIds are set.");
            }
            if (shardsTotal <= 0) {
                throw new IllegalStateException("shardsTotal is not set");
            }
            return new QueryInfo(this);
        }
        
    }

    private QueryInfo(Builder builder) {
        this.index = builder.index;
        this.started = builder.started;
        this.queryType = builder.queryType;
        this.resultsType = builder.resultsType;
        this.queryString = builder.queryString;
        this.numBlobIds = builder.numBlobIds;
        this.shardsTotal = builder.shardsTotal;
        this.shardsComplete = builder.shardsComplete;
        this.originThreadName = builder.originThreadName;
        this.finished = builder.finished;
        this.resultSize = builder.resultSize;
        this.results = builder.results;
        this.error = builder.error;
    }

    public long getIndex() {
        return index;
    }

    public long getStarted() {
        return started;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public ResultsType getResultsType() {
        return resultsType;
    }

    public String getQueryString() {
        return queryString;
    }

    public int getNumBlobIds() {
        return numBlobIds;
    }

    public int getShardsTotal() {
        return shardsTotal;
    }

    public int getShardsComplete() {
        return shardsComplete;
    }

    public String getOriginThreadName() {
        return originThreadName;
    }

    public long getFinished() {
        return finished;
    }

    public long getResultSize() {
        return resultSize;
    }

    public long getResults() {
        return results;
    }

    public String getError() {
        return error;
    }

    @Override
    public int compareTo(QueryInfo o) {
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

    @JsonIgnore
	public long getSizeEstimate() {
		return BASE_SIZE_ESTIMATE + originThreadName.length() + queryString.length() + (error == null ? 0 : error.length());
	}
}