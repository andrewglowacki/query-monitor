package sos.accumulo.monitor.data;

import java.util.Map;

public class GeneralStatus {
    private Map<String, ExecutorStatus> executors;
    private Map<String, QueryRunnerStatus> queryRunners;

    public Map<String, ExecutorStatus> getExecutors() {
        return executors;
    }

    public void setExecutors(Map<String, ExecutorStatus> executors) {
        this.executors = executors;
    }

    public Map<String, QueryRunnerStatus> getQueryRunners() {
        return queryRunners;
    }

    public void setQueryRunners(Map<String, QueryRunnerStatus> queryRunners) {
        this.queryRunners = queryRunners;
    }
    
}