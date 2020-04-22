package sos.accumulo.monitor.data;

import java.util.List;

public class QueryRunnerStatus {
    private RunnerHealth health;
    private int running;
    private long finished;
    private List<ErrorInfo> recentErrors;
    private long lastHeard;
    
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

    public List<ErrorInfo> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(List<ErrorInfo> recentErrors) {
        this.recentErrors = recentErrors;
    }

}