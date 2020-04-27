package sos.accumulo.monitor.data;

public class QueryRunnerMatch {

    private boolean found;
    private String name;
    private String shard;
    private long query;
    private String attemptServer;
    private long attemptStarted;

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShard() {
        return shard;
    }

    public void setShard(String shard) {
        this.shard = shard;
    }

    public long getQuery() {
        return query;
    }

    public void setQuery(long query) {
        this.query = query;
    }

    public String getAttemptServer() {
        return attemptServer;
    }

    public void setAttemptServer(String attemptServer) {
        this.attemptServer = attemptServer;
    }

    public long getAttemptStarted() {
        return attemptStarted;
    }

    public void setAttemptStarted(long attemptStarted) {
        this.attemptStarted = attemptStarted;
    }

    
}