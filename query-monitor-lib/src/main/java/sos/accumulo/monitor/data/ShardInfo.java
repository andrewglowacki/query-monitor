package sos.accumulo.monitor.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ShardInfo {
    private static final int BASE_SIZE_ESTIMATE = 8 * 4;
    private String shard;
    private List<AttemptInfo> failedAttempts = new ArrayList<>(0);
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

    @JsonIgnore
	public long getSizeEstimate() {
        long failedSize = (8 * failedAttempts.size());
        for (AttemptInfo attempt : failedAttempts) {
            failedSize += attempt.getSizeEstimate();
        }
		return BASE_SIZE_ESTIMATE + shard.length() + failedSize + latestAttempt.getSizeEstimate();
	}

    
}