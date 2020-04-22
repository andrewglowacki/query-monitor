package sos.accumulo.monitor.data;

public class AttemptInfo {
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
    
}