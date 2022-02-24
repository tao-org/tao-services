package ro.cs.tao.services.entity.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.topology.Workspace;

@Controller
@RequestMapping("/workspace")
public class WorkspaceController extends DataEntityController<Workspace, Integer, WorkspaceService> {

}
