package sos.accumulo.monitor.tracker;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TrackerAddress implements ApplicationListener<WebServerInitializedEvent> {

    @Value("${server.address:}")
    private volatile String host;

    private volatile int port;

    @PostConstruct
    public void setup() {
        if (host.isEmpty()) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        port = event.getSource().getPort();
    }

    public String get() {
        if (port == 0) {
            // wait up to 30 seconds for the server to start.
            for (int i = 0; i < 300; i++) {
                if (port > 0) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { 
                    return null;
                }
            }
        }
        return host + ":" + port;
    }
}