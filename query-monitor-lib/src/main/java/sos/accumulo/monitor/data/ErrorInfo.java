package sos.accumulo.monitor.data;

public class ErrorInfo implements Comparable<ErrorInfo> {
    private long time;
    private String error = "";

    public static ErrorInfo create(String error) {
        ErrorInfo info = new ErrorInfo();
        if (error != null) {
            info.setError(error);
        }
        info.setTime(System.currentTimeMillis());
        return info;
    }

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

    @Override
    public int compareTo(ErrorInfo o) {
        int diff = Long.compare(time, o.time);
        if (diff != 0) {
            return diff;
        }
        return error.compareTo(o.error);
    }
    
}