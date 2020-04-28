package sos.accumulo.monitor.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AttemptInfo {
    private static final long BASE_SIZE_ESTIMATE = (8 * 9) + 4;
    private String server;
    private long started;
    private long indexFinished;
    private long dataFinished;
    private long finished;
    private String error;
    private long indexResults;
    private long dataResults;
    private long dataSize;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public long getIndexFinished() {
        return indexFinished;
    }

    public void setIndexFinished(long indexFinished) {
        this.indexFinished = indexFinished;
    }

    public long getDataFinished() {
        return dataFinished;
    }

    public void setDataFinished(long dataFinished) {
        this.dataFinished = dataFinished;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getIndexResults() {
        return indexResults;
    }

    public void setIndexResults(long indexResults) {
        this.indexResults = indexResults;
    }

    public long getDataResults() {
        return dataResults;
    }

    public void setDataResults(long dataResults) {
        this.dataResults = dataResults;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    @JsonIgnore
	public long getSizeEstimate() {
		return BASE_SIZE_ESTIMATE + server.length() + (error == null ? 0 : error.length());
	}
    
}