package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;

@Profile({ "TrackerModeRunner", "TrackerModeProxy" })
@RestController
public class RunnerController {

    @Autowired
    private RunnerTracker tracker;
    
    @Value("${server.address:}")
    private String trackerHost;

    @Value("${runner.name:}")
    private String runnerName;

    @Autowired
    private AnnounceDao announceDao;

    private volatile ScheduledExecutorService announceThread = null;

    @PostConstruct
    public void setup() {
        if (!System.getProperty("spring.profiles.active", "").equals("TrackerModeRunner")) {
            return;
        }

        if (runnerName.isEmpty()) {
            throw new RuntimeException("runner.name must be set for runner tracker.");
        }

        announceThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
        announceThread.scheduleWithFixedDelay(announceDao::announceRunner, 0, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void cleanup() {
        if (announceThread != null) {
            announceThread.shutdownNow();
        }
    }

    @GetMapping("/running")
    public Set<QueryInfo> getRunning() {
        return tracker.getRunning();
    }

    @GetMapping("/finished")
    public Set<QueryInfo> getFinished() {
        return tracker.getFinished();
    }

    @PostMapping("/proxy/start")
    public long proxyRegister(@RequestParam String address, @RequestParam String id) {
        return tracker.registerProxy(address, id);
    }

    @PostMapping("/proxy/finished")
    @ResponseStatus(value = HttpStatus.OK)
    public void proxyFinished(@RequestBody QueryInfoDetail detail) {
        tracker.proxyFinished(detail);
    }

    @PostMapping("/proxy/error")
    @ResponseStatus(value = HttpStatus.OK)
    public void proxyError(@RequestParam String error) {
        tracker.recordError(error);
    }

    @GetMapping("/status")
    public QueryRunnerStatus getStatus() {
        return tracker.createStatus();
    }

    @GetMapping("/scans")
    public List<AccumuloScanInfo> getScans() {
        List<AccumuloScanInfo> scans = AccumuloScanInfo.listScans();
        scans.addAll(tracker.getProxyScans());
        return scans;
    }

    @GetMapping("/query/{index}")
    public QueryInfoDetail getQueryDetail(@PathVariable long index) throws IOException {
        return tracker.getByIndex(index);
    }
    
    @PostMapping("/find")
    public QueryRunnerMatch findQueryDetail(
        @RequestParam String queryString, 
        @RequestParam String shard, 
        @RequestParam long started) {
        QueryRunnerMatch match = tracker.findMatch(queryString, shard, started);
        if (match.isFound()) {
            match.setName(runnerName);
        }
        return match;
    }
}