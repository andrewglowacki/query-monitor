package sos.accumulo.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.ExecutorShardInfo;
import sos.accumulo.monitor.data.ExecutorShardInfoDetail;
import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.util.HttpQuery;

@RestController
@RequestMapping("/api/executor")
public class ExecutorController {

    private static final JavaType SCAN_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
    private static final JavaType SHARD_INFO_LIST = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, ExecutorShardInfo.class);

    @Autowired
    private GeneralStatusDao statusDao;

    @Description("Gets detailed executor status")
    @GetMapping("/{name}/status")
    public ExecutorStatusDetail getExecutorStatus(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        ExecutorStatusDetail status = HttpQuery.normalQuery("http://" + address + "/status-detail", ExecutorStatusDetail.class);
        statusDao.updateNow(name, status);
        return status;
    }

    @Description("Gets all the currently running shards")
    @GetMapping("/{name}/running")
    public Set<ExecutorShardInfo> getRunningShards(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/running", SHARD_INFO_LIST);
    }

    @Description("Gets the recently finished shards")
    @GetMapping("/{name}/finished")
    public Set<ExecutorShardInfo> getFinishedShards(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/finished", SHARD_INFO_LIST);
    }

    @Description("Gets all the currently running scans on this executor")
    @GetMapping("/{name}/scans")
    public List<AccumuloScanInfo> getAccumuloScans(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/scans", SCAN_LIST);
    }

    @Description("Gets detailed information for the specified shard index")
    @GetMapping("/{name}/shard/{index}")
    public ExecutorShardInfoDetail getShardInfoDetail(@PathVariable String name, @PathVariable long index) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/shard/" + index, ExecutorShardInfoDetail.class);
    }

    @Description("Finds a corresponding shard that matches as closely as possible to the provided parameters")
    @PostMapping("/{name}/find")
    public ExecutorShardInfoDetail findShardInfoDetail(
        @PathVariable String name, 
        @RequestParam long started, 
        @RequestParam String shard, 
        @RequestParam String queryString) throws IOException {
        
        String address = statusDao.getAddress(name);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("started", "" + started));
        params.add(new BasicHeader("shard", shard));
        params.add(new BasicHeader("queryString", queryString));

        return HttpQuery.normalPostQuery("http://" + address + "/find", params, ExecutorShardInfoDetail.class);
    }
}