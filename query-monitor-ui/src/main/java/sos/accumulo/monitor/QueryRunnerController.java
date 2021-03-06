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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryRunnerStatus;
import sos.accumulo.monitor.util.HttpQuery;

@RestController
@RequestMapping("/api/runner")
public class QueryRunnerController {

    private static final JavaType SCAN_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
    private static final JavaType QUERY_INFO_LIST = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, QueryInfo.class);

    @Autowired
    private GeneralStatusDao statusDao;

    @Description("Registers a query runner with the monitor. This will overwrite other query runners with the same name.")
    @PostMapping(value = "/register")
    @ResponseStatus(value = HttpStatus.OK)
    public void registerQueryRunner(@RequestParam String name,  @RequestParam String address) {
        statusDao.register(name, address);
    }

    @GetMapping("/{name}/running")
    public Set<QueryInfo> getRunningQueries(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/running", QUERY_INFO_LIST);
    }

    @GetMapping("/{name}/finished")
    public Set<QueryInfo> getFinishedQueries(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/finished", QUERY_INFO_LIST);
    }

    @Description("Gets an updated query runner status")
    @GetMapping("/{name}/status")
    public QueryRunnerStatus getQueryRunnerStatus(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        QueryRunnerStatus status = HttpQuery.normalQuery("http://" + address + "/status", QueryRunnerStatus.class);
        statusDao.updateNow(name, status);
        return status;
    }

    @Description("Gets all the currently running scans on this runner")
    @GetMapping("/{name}/scans")
    public List<AccumuloScanInfo> getAccumuloScans(@PathVariable String name) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/scans", SCAN_LIST);
    }

    @Description("Gets the detailed query info for the specified query index")
    @GetMapping("/{name}/query/{index}")
    public QueryInfoDetail getQueryInfoDetail(@PathVariable String name, @PathVariable long index) throws IOException {
        String address = statusDao.getAddress(name);
        return HttpQuery.normalQuery("http://" + address + "/query/" + index, QueryInfoDetail.class);
    }

    @Description("Finds the query runner that matches as closely as possible to the provided parameters")
    @PostMapping("/find")
    public QueryRunnerMatch findSourceRunner(
        @RequestParam String server,
        @RequestParam String shard,
        @RequestParam String queryString,
        @RequestParam long started) throws IOException {

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("shard", shard));
        params.add(new BasicHeader("queryString", queryString));
        params.add(new BasicHeader("started", "" + started));

        Set<String> candidates = statusDao.getRunnersOnServer(server);
        for (String runner : candidates) {
            String address = statusDao.getAddress(runner);
            QueryRunnerMatch match = HttpQuery.normalPostQuery("http://" + address + "/find", params, QueryRunnerMatch.class);
            if (match != null && match.isFound()) {
                match.setName(runner);
                return match;
            }
        }
        return new QueryRunnerMatch();
    }
}