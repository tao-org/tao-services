package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.SortDirection;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;
import ro.cs.tao.utils.StringUtilities;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workspace")
public class WorkspaceEntityController extends DataEntityController<Workspace, Integer, WorkspaceService> {

    @RequestMapping(value = "/types/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkspaceTypes() {
        try {
            return prepareResult(Arrays.stream(WorkspaceType.values()).map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Optional<String> sortByField, Optional<SortDirection> sortDirection) {
        if (!(pageNumber.isPresent() || pageSize.isPresent() || sortByField.isPresent() || sortDirection.isPresent())) {
            try {
                return prepareResult(this.service.getByUser(currentUser()));
            } catch (Exception e) {
                return handleException(e);
            }
        } else{
            return super.list(pageNumber, pageSize, sortByField, sortDirection);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> save(Workspace entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserName())) {
                entity.setUserName(currentUser());
            }
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(Integer id, Workspace entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserName())) {
                throw new IllegalArgumentException("Cannot update an unassigned workspace");
            }
            return super.save(entity);
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
