package sos.accumulo.monitor.data;

import java.util.List;

public class ShardInfo {
    private String shard;
    private List<AttemptInfo> failedAttempts;
    private AttemptInfo latestAttempt;

    public String getShard() {
        return shard;
    }

    public void setShard(String shard) {
        this.shard = shard;
    }

    public List<AttemptInfo> getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(List<AttemptInfo> failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public AttemptInfo getLatestAttempt() {
        return latestAttempt;
    }

    public void setLatestAttempt(AttemptInfo latestAttempt) {
        this.latestAttempt = latestAttempt;
    }

    
}