package ro.cs.tao.services.monitoring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.monitoring.interfaces.MonitoringService;
import ro.cs.tao.services.monitoring.model.Snapshot;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/monitor")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;

    @RequestMapping(value = "/master", method = RequestMethod.GET)
    public ResponseEntity<?> getMasterSnapshot() {
        final Snapshot snapshot = monitoringService.getMasterSnapshot();
        if (snapshot == null) {
            return new ResponseEntity<>(new ServiceError("No information available for master node"),
                                        HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(snapshot, HttpStatus.OK);
    }

    @RequestMapping(value = "/notification", method = RequestMethod.GET)
    public ResponseEntity<?> getNotifications() {
        return new ResponseEntity<>(monitoringService.getNotifications(), HttpStatus.OK);
    }

}
