package sos.accumulo.monitor.tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import sos.accumulo.monitor.util.HttpQuery;

@Repository
public class MonitorDaoImpl implements MonitorDao {

    @Value("${announce.address}")
    private String announceAddress;

    @Override
    public String getAnnounceAddress() {
        return announceAddress;
    }

    @Override
    public void announceRunner(String name, String trackerAddress) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicHeader("name", name));
        params.add(new BasicHeader("address", trackerAddress));
        HttpQuery.normalPostQuery("http://" + announceAddress + "/api/runner/register", params);
    }
}