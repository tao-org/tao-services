package ro.cs.tao.services.entity.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.WorkflowDescriptor;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/workflow")
public class WorkflowController extends DataEntityController<WorkflowDescriptor, WorkflowService> {

}
