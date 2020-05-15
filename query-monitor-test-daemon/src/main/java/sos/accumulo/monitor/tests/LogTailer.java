package sos.accumulo.monitor.tests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;

public class LogTailer implements Runnable {

    private final BufferedReader reader;
    private final Logger log;
    private volatile boolean webReady = false;
    private volatile boolean trackerReady = false;
    
    public LogTailer(Logger log, InputStream inStream) {
        this.log = log;
        this.reader = new BufferedReader(new InputStreamReader(inStream));
    }

    public boolean isWebReady() {
        return webReady;
    }
    public boolean isTrackerReady() {
        return trackerReady && webReady;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Tomcat started on port(s):")) {
                    webReady = true;
                } else if (line.equals("Fake Tracker Ready - test data populated")) {
                    trackerReady = true;
                }
                log.info(line);
            }
        } catch (Throwable ex) { }
    }
    
}