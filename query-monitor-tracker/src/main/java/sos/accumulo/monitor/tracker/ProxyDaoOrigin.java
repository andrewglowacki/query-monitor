package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.util.HttpQuery;

@Profile("TrackerModeRunner")
@Repository
public class ProxyDaoOrigin implements ProxyDao {
    
    private static final JavaType SCAN_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, AccumuloScanInfo.class);
    private static final JavaType QUERY_INFO_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, QueryInfo.class);
    private static final Logger log = LoggerFactory.getLogger(ProxyDaoOrigin.class);

    @Override
    public long startProxyQuery() {
        return -1;
    }

    @Override
    public void finishProxyQuery(ProxyQuery query) {
        // NOOP
    }

    @Override
    public boolean recordError(String error) {
        // NOOP
        return false;
    }

    @Override
    public QueryInfoDetail getQueryDetail(String address, long index) throws IOException {
        return HttpQuery.normalQuery("http://" + address + "/query/" + index, QueryInfoDetail.class);
    }

    @Override
    public QueryRunnerMatch findMatch(String queryString, String shard, long started,
            Collection<RegisteredProxy> proxies, QueryRunnerMatch match) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("queryString", queryString));
        params.add(new BasicHeader("shard", shard));
        params.add(new BasicHeader("started", "" + started));
        for (RegisteredProxy proxy : proxies) {
            try {
                QueryRunnerMatch thisMatch = HttpQuery.normalPostQuery("http://" + proxy.getAddress() + "/find", params, QueryRunnerMatch.class);
                if (thisMatch.isFound() && thisMatch.getAttemptStarted() > match.getAttemptStarted()) {
                    match = thisMatch;
                    if (match.getAttemptStarted() == started) {
                        return match;
                    }
                }
            } catch (IOException ex) {
                log.error("Failed to find match from proxy: " + proxy.getAddress(), ex);
            }
        }
        return match;
    }

    @Override
    public List<QueryInfo> getRunningQueries(String address) throws IOException {
        return HttpQuery.normalQuery("http://" + address + "/running", QUERY_INFO_LIST);
    }

    @Override
    public List<AccumuloScanInfo> getRunningScans(String address) throws IOException {
        return HttpQuery.normalQuery("http://" + address + "/scans", SCAN_LIST);
    }
}