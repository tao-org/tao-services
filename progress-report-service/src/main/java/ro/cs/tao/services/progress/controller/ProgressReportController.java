package ro.cs.tao.services.progress.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.messaging.TaskProgress;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.ProgressReportService;

import java.util.List;

@Controller
@RequestMapping("/progress")
public class ProgressReportController extends BaseController {

    @Autowired
    private ProgressReportService progressReportService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<TaskProgress>> getTasksInProgress() {
        return new ResponseEntity<>(progressReportService.getRunningTasks(), HttpStatus.OK);
    }

}
