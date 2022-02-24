package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;

import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workspace")
public class WorkspaceController extends DataEntityController<Workspace, Integer, WorkspaceService> {

    @RequestMapping(value = "/types/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkspaceTypes() {
        try {
            return prepareResult(Arrays.stream(WorkspaceType.values()).map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /*@RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> save(WorkspaceBean entity) {
        return super.save(ServiceTransformUtils.fromBean(entity));
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> update(WorkspaceBean entity) {
        return super.update(entity.getId(), ServiceTransformUtils.fromBean(entity));
    }*/
}
