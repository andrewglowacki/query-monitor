package sos.accumulo.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sos.accumulo.monitor.data.GeneralStatus;


@RestController
@RequestMapping("/api")
public class MonitorController {

    @Autowired
    private GeneralStatusDao statusDao;

    @Description("Gets the available executors and query runners and their statuses")
    @GetMapping("/status")
    public GeneralStatus getGeneralStatus() {
        return statusDao.getGeneralStatus();
    }

}