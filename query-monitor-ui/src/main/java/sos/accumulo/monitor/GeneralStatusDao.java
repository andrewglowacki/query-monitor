package sos.accumulo.monitor;

import java.util.Set;

import sos.accumulo.monitor.data.ExecutorStatusDetail;
import sos.accumulo.monitor.data.GeneralStatus;
import sos.accumulo.monitor.data.QueryRunnerStatus;

public interface GeneralStatusDao {

	public GeneralStatus getGeneralStatus();

	public void updateNow(String name, ExecutorStatusDetail status);

	public void updateNow(String name, QueryRunnerStatus status);

	public void updateAllNow();

	public void register(String name, String address);

	public String getAddress(String name);

	public Set<String> getRunnersOnServer(String server);

}