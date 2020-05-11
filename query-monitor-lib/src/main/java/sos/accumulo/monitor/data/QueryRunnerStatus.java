package sos.accumulo.monitor.data;

import java.util.Set;

public class QueryRunnerStatus {
    private RunnerHealth health;
    private int running;
    private long finished;
    private Set<ErrorInfo> recentErrors;
    private long lastHeard = System.currentTimeMillis();
    
    public long getLastHeard() {
        return lastHeard;
    }
    
    public void setLastHeard(long lastHeard) {
        this.lastHeard = lastHeard;
    }

    public RunnerHealth getHealth() {
        return health;
    }

    public void setHealth(RunnerHealth health) {
        this.health = health;
    }

    public int getRunning() {
        return running;
    }

    public void setRunning(int running) {
        this.running = running;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public Set<ErrorInfo> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(Set<ErrorInfo> recentErrors) {
        this.recentErrors = recentErrors;
    }

}