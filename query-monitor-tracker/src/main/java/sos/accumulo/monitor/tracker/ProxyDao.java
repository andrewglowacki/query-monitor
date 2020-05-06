package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import sos.accumulo.monitor.data.AccumuloScanInfo;
import sos.accumulo.monitor.data.QueryInfo;
import sos.accumulo.monitor.data.QueryInfoDetail;
import sos.accumulo.monitor.data.QueryRunnerMatch;

public interface ProxyDao {
    public long startProxyQuery() throws IOException;

	public void finishProxyQuery(ProxyQuery query) throws IOException;

	public boolean recordError(String error);

	public QueryInfoDetail getQueryDetail(String address, long index) throws IOException;

	public QueryRunnerMatch findMatch(String queryString, String shard, long started, Collection<RegisteredProxy> proxies, QueryRunnerMatch match);

	public List<QueryInfo> getRunningQueries(String address) throws IOException;

	public List<AccumuloScanInfo> getRunningScans(String address) throws IOException;
}