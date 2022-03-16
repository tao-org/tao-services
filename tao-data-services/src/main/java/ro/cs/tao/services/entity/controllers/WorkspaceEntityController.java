package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
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
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Workspace entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserName())) {
                entity.setUserName(currentUser());
            }
            entity.setSystem(false);
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") Integer id, @RequestBody Workspace entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserName())) {
                throw new IllegalArgumentException("Cannot update an unassigned workspace");
            }
            entity.setSystem(false);
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
