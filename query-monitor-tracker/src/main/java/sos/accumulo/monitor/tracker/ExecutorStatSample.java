package sos.accumulo.monitor.tracker;

public class ExecutorStatSample implements Comparable<ExecutorStatSample> {

    private final long time = System.currentTimeMillis();
    private final long finished;
    private final long results;

    public ExecutorStatSample(long finished, long results) {
        this.finished = finished;
        this.results = results;
    }

    public long getTime() {
        return time;
    }
    public long getFinished() {
        return finished;
    }
    public long getResults() {
        return results;
    }

    @Override
    public int compareTo(ExecutorStatSample o) {
        return Long.compare(time, o.time);
    }
}