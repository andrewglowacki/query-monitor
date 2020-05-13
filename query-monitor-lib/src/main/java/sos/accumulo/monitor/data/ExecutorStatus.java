package sos.accumulo.monitor.data;

public class ExecutorStatus {
    private RunnerHealth health;
    private int running;
    private long finished;
    private int recentErrorCount;
    private long lastHeard = System.currentTimeMillis();
    
    public ExecutorStatus() { }

    public ExecutorStatus(ExecutorStatusDetail copy) {
        this.recentErrorCount = copy.getRecentErrors() != null ? copy.getRecentErrors().size() : copy.getRecentErrorCount();
        this.health = copy.getHealth();
        this.running = copy.getRunning();
        this.finished = copy.getFinished();
        this.lastHeard = copy.getLastHeard();
    }

    public void setRecentErrorCount(int recentErrorCount) {
        this.recentErrorCount = recentErrorCount;
    }
    
    public int getRecentErrorCount() {
        return recentErrorCount;
    }

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
    
}