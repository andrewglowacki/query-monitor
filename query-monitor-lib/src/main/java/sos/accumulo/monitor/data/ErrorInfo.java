package sos.accumulo.monitor.data;

public class ErrorInfo {
    private long time;
    private String error;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
}