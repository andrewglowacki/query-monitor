package sos.accumulo.monitor.data;

import java.util.List;

public class ExecutorStatusDetail extends ExecutorStatus {

    private List<ErrorInfo> recentErrors;
    private List<Long> statTimes;
    private List<Long> finishedStats;
    private List<Long> resultStats;

    public List<ErrorInfo> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(List<ErrorInfo> recentErrors) {
        this.recentErrors = recentErrors;
    }

    public List<Long> getStatTimes() {
        return statTimes;
    }

    public void setStatTimes(List<Long> statTimes) {
        this.statTimes = statTimes;
    }

    public List<Long> getFinishedStats() {
        return finishedStats;
    }

    public void setFinishedStats(List<Long> finishedStats) {
        this.finishedStats = finishedStats;
    }

    public List<Long> getResultStats() {
        return resultStats;
    }

    public void setResultStats(List<Long> resultStats) {
        this.resultStats = resultStats;
    }

}