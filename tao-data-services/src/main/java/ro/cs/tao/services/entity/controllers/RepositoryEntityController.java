package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.SortDirection;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.RepositoryBean;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workspace")
public class RepositoryEntityController extends DataEntityController<Repository, String, RepositoryService> {

    @RequestMapping(value = "/types/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkspaceTypes() {
        try {
            return prepareResult(Arrays.stream(RepositoryType.values()).map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
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
                return prepareResult(this.service.getByUser(currentUser()).stream().map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
            } catch (Exception e) {
                return handleException(e);
            }
        } else{
            return prepareResult(this.service.list().stream().map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        try {
            final Repository entity = this.service.findById(id);
            if (entity == null || !entity.getUserName().equals(currentUser())) {
                throw new IllegalArgumentException("You cannot access this workspace");
            }
            final RepositoryBean bean = ServiceTransformUtils.toBean(entity);
            final StorageService storageService = StorageServiceFactory.getInstance(entity);
            List<ItemAction> actions = storageService.getRegisteredActions();
            if (actions != null) {
                bean.setActions(actions.stream().map(ItemAction::name).collect(Collectors.toList()));
            }
            return prepareResult(bean);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Repository entity) {
        try {
            entity.setUserName(currentUser());
            entity.setSystem(false);
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody Repository entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserName()) ||
                !entity.getUserName().equals(currentUser())) {
                throw new IllegalArgumentException("Cannot update this workspace");
            }
            entity.setSystem(false);
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
