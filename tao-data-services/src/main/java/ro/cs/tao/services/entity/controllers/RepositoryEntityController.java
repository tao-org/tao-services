package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
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
        final List<RepositoryBean> repositories = new ArrayList<>();
        if (!(pageNumber.isPresent() || pageSize.isPresent() || sortByField.isPresent() || sortDirection.isPresent())) {
            try {
                List<Repository> userRepositories = this.service.getByUser(currentUser());
                for (Repository repository : userRepositories) {
                    final RepositoryBean bean = ServiceTransformUtils.toBean(repository);
                    final StorageService storageService = StorageServiceFactory.getInstance(repository);
                    List<ItemAction> actions = storageService.getRegisteredActions();
                    if (actions != null) {
                        bean.setActions(actions.stream().map(a -> new RepositoryBean.Action(a.name(), a.supportedFiles()))
                                               .sorted(Comparator.comparing(RepositoryBean.Action::getName)).collect(Collectors.toList()));
                    }
                    repositories.add(bean);
                }
                return prepareResult(repositories);
            } catch (Exception e) {
                return handleException(e);
            }
        } else{
            final List<Repository> list = this.service.list();
            final int pNo = pageNumber.orElse(0);
            final int pSize = pageSize.orElse(list.size());
            final int start = pNo * pSize;
            final int end = Math.min((pNo + 1) * pSize, list.size());
            for (Repository repository : list.subList(start, end)) {
                final RepositoryBean bean = ServiceTransformUtils.toBean(repository);
                final StorageService storageService = StorageServiceFactory.getInstance(repository);
                List<ItemAction> actions = storageService.getRegisteredActions();
                if (actions != null) {
                    bean.setActions(actions.stream().map(a -> new RepositoryBean.Action(a.name(), a.supportedFiles()))
                                           .sorted(Comparator.comparing(RepositoryBean.Action::getName)).collect(Collectors.toList()));
                }
                repositories.add(bean);
            }
            if (sortByField.isPresent() || sortDirection.isPresent()) {
                final boolean asc = sortDirection.orElse(SortDirection.ASC).equals(SortDirection.ASC);
                repositories.sort((o1, o2) -> asc ? o1.getName().compareTo(o2.getName()) : o2.getName().compareTo(o1.getName()));
            }
            return prepareResult(repositories);
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
                bean.setActions(actions.stream().map(a -> new RepositoryBean.Action(a.name(), a.supportedFiles()))
                                       .sorted(Comparator.comparing(RepositoryBean.Action::getName)).collect(Collectors.toList()));
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
