package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;
import sos.accumulo.monitor.data.QueryInfo.Builder;
import sos.accumulo.monitor.util.HttpQuery;

@Profile("TrackerModeProxy")
@Repository
public class ProxyDaoTarget implements ProxyDao {

    private static final Logger log = LoggerFactory.getLogger(ProxyDaoTarget.class);

    @Value("${origin.proxy.address}")
    private String originProxyServer;

    @LocalServerPort
    private int localPort;

    @Value("${proxy.id}")
    private String proxyId;

    private String localHostAddress = InetAddress.getLoopbackAddress().getHostAddress();

    @Override
    public ProxyQuery startProxyQuery(Builder builder) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("address", localHostAddress + ":" + localPort));
        params.add(new BasicHeader("id", proxyId));
        return new ProxyQuery(HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/start", params, Long.class), builder);
    }

    @Override
    public void finishProxyQuery(ProxyQuery query) throws IOException {
        HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/finished", query.getDetail());
    }

    @Override
    public boolean recordError(String error) {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicHeader("error", error));
            HttpQuery.normalPostQuery("http://" + originProxyServer + "/proxy/error", params);
        } catch (IOException ex) {
            log.warn("Failed to report error to proxy origin server", ex);
        }
        return true;
    }

    @Override
    public QueryInfoDetail getQueryDetail(String address, long index) {
        log.error("Proxy tracker should not be retrieving query detail from a sub-proxy.");
        return null;
    }

    @Override
    public QueryRunnerMatch findMatch(String queryString, String shard, long started,
            Collection<RegisteredProxy> proxies, QueryRunnerMatch match) {
        log.error("Proxy tracker should not be finding a match from a sub-proxy.");
        return match;
    }

    @Override
    public List<QueryInfo> getRunningQueries(String address) {
        log.error("Proxy tracker should not have sub-proxies to get running queries from.");
        return new ArrayList<>(0);
    }

    @Override
    public List<AccumuloScanInfo> getRunningScans(String address) throws IOException {
        log.error("Proxy tracker should not have sub-proxies to get running scans from.");
        return new ArrayList<>(0);
    }
}