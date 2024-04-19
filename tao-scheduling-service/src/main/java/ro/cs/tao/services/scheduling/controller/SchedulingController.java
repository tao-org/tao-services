package ro.cs.tao.services.scheduling.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.SchedulingService;
import ro.cs.tao.services.model.scheduling.SchedulingInfo;
import ro.cs.tao.services.scheduling.beans.SchedulingRequest;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/scheduling")
@Tag(name = "Scheduled Executions", description = "Endpoint for managing scheduled executions")
public class SchedulingController extends BaseController {

    @Autowired
    private SchedulingService schedulingService;

    /**
     * Retrieves all the scheduled executions made by users
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> listUserSchedules() {
        try {
        	List<SchedulingInfo> schedules = schedulingService.listUserSchedules();
        	if (schedules == null) {
        		schedules = new ArrayList<>();
        	}
        	
            return prepareResult(schedules);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Creates a new scheduled execution.
     * @param request   The structure defining the schedule
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addSchedule(@RequestBody SchedulingRequest request) {
    	ResponseEntity<ServiceResponse<?>> response;
    	if (request.getId() != null) {
    		return prepareResult("No id should be specified for a new schedule", ResponseStatus.FAILED);
    	}
        try {
        	final String id = schedulingService.addExecutionSchedule(
        			request.getName(), 
        			request.getStartTime(), 
        			request.getRepeatInterval(), 
        			request.getWorkflowId(), 
        			request.getParameters(),
        			request.getMode());
        	
            response = prepareResult("Successfully started schedule with id: " + id, ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            response = handleException(ex);
        }
        
        return response;
    }

    /**
     * Removes a scheduled execution. If the scheduled execution does not belong to the caller,
     * a 405 response is returned.
     * @param id    The identifier of the scheduled execution
     */
    @RequestMapping(value = "/remove", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> removeSchedule(@RequestParam("id") String id) {
        try {
        	if (schedulingService.removeExecutionSchedule(id)) {
        		return prepareResult("Schedule removed successfully", ResponseStatus.SUCCEEDED);
        	}
        	
            return prepareResult("Cannot remove schedule!", ResponseStatus.FAILED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }
    /**
     * Suspends the schedule of an execution.
     * @param id    The identifier of the scheduled execution
     */
    @RequestMapping(value = "/pause", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> pauseSchedule(@RequestParam("id") String id) {
        try {
        	schedulingService.pauseExecutionSchedule(id);
        	return prepareResult("Schedule paused successfully", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }
    /**
     * Resumes the schedule of an execution.
     * @param id    The identifier of the scheduled execution
     */
    @RequestMapping(value = "/resume", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> resumeSchedule(@RequestParam("id") String id) {
        try {
        	schedulingService.resumeExecutionSchedule(id);
        	return prepareResult("Schedule resumed successfully", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }
    /**
     * Updates the parameters of an existing scheduled execution.
     * @param id    The identifier of the scheduled execution
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateSchedule(@RequestBody SchedulingRequest request) {
    	ResponseEntity<ServiceResponse<?>> response;
    	if (request.getId() == null) {
    		return prepareResult("Scheduler identifier required for update operation", ResponseStatus.FAILED);
    	}
        try {
        	final String id = schedulingService.updateExecutionSchedule(
        			request.getId(),
        			request.getName(), 
        			request.getStartTime(), 
        			request.getRepeatInterval(), 
        			request.getWorkflowId(), 
        			request.getParameters(),
        			request.getMode());
        	
            response = prepareResult("Successfully updated schedule with id: " + id, ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            response = handleException(ex);
        }
        
        return response;
    }

}
