package sos.accumulo.monitor.tracker;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatus;
import sos.accumulo.monitor.data.ExecutorStatusDetail;


@Profile("TrackerModeExecutor")
@RestController
public class ExecutorController {

    @Autowired
    private ExecutorTracker tracker;

    @GetMapping("/running")
    public Set<ExecutorShardInfo> getRunning() {
        return tracker.getRunning();
    }
    
    @GetMapping("/finished")
    public Set<ExecutorShardInfo> getFinished() {
        return tracker.getFinished();
    }
    
    @GetMapping("/status")
    public ExecutorStatus getStatus() {
        return tracker.createStatus();
    }
    
    @GetMapping("/status-detail")
    public ExecutorStatusDetail getStatusDetail() {
        return tracker.createStatusDetail();
    }
    
    @GetMapping("/scans")
    public List<AccumuloScanInfo> getScans() {
        return AccumuloScanInfo.listScans();
    }
    
    @GetMapping("/shard/{index}")
    public ExecutorShardInfoDetail getShardDetail(@PathVariable long index) {
        return tracker.getByIndex(index);
    }
    
    @PostMapping("/find")
    public ExecutorShardInfoDetail findShardDetail(
        @RequestParam String queryString,
        @RequestParam String shard, 
        @RequestParam long started) {
        return tracker.findMatch(queryString, shard, started);
    }
    
}