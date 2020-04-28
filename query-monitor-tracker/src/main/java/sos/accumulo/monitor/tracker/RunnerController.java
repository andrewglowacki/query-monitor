package sos.accumulo.monitor.tracker;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;

@Profile("TrackerModeRunner")
@RestController
public class RunnerController {

    private final RunnerTracker tracker = RunnerTracker.getInstance();

    @GetMapping("/running")
    public Set<QueryInfo> getRunning() {
        return tracker.getRunning();
    }
    
    @GetMapping("/finished")
    public Set<QueryInfo> getFinished() {
        return tracker.getFinished();
    }
    
    @GetMapping("/status")
    public QueryRunnerStatus getStatus() {
        return tracker.createStatus();
    }
    
    @GetMapping("/scans")
    public List<AccumuloScanInfo> getScans() {
        return AccumuloScanInfo.listScans();
    }
    
    @GetMapping("/shard/{index}")
    public QueryInfoDetail getQueryDetail(@PathVariable long index) {
        return tracker.getByIndex(index);
    }
    
    @PostMapping("/find")
    public QueryRunnerMatch findQueryDetail(
        @RequestParam String queryString, 
        @RequestParam String shard, 
        @RequestParam long started) {
        return tracker.findMatch(queryString, shard, started);
    }
}