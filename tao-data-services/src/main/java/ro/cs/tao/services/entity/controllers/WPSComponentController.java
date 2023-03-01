package ro.cs.tao.services.entity.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.WPSComponent;
import ro.cs.tao.component.WPSComponentBean;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.interfaces.WPSComponentService;
import ro.cs.tao.utils.JacksonUtil;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wpscomponent")
public class WPSComponentController extends BaseController {

    @Autowired
    private ContainerService containerService;
    @Autowired
    private WPSComponentService wpsComponentService;
    @Autowired
    private RepositoryService repositoryService;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            //return prepareResult(ServiceTransformUtils.toWPSComponentInfos(wpsComponentService.list(pageNumber, pageSize, sort)));
            return prepareResult(wpsComponentService.list(pageNumber, pageSize, sort).stream().map(WPSComponentBean::new).collect(Collectors.toList()));
        } else {
            //return prepareResult(ServiceTransformUtils.toWPSComponentInfos(wpsComponentService.list()));
            return prepareResult(wpsComponentService.list().stream().map(WPSComponentBean::new).collect(Collectors.toList()));
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody WPSComponentBean entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("Emtpy body");
            }
            if (entity.getOwner() == null || entity.getOwner().isEmpty()) {
                entity.setOwner(currentUser());
            }
            final Container container = containerService.findById(entity.getServiceId());
            if (container == null) {
                throw new IllegalArgumentException("No WPS service associated");
            }
            final WPSComponent component = entity.toComponent();
            component.setService(container);
            final List<SourceDescriptor> sources = component.getSources();
            if (sources != null) {
                sources.forEach(s -> s.setParentId(component.getId()));
            }
            final List<TargetDescriptor> targets = component.getTargets();
            if (targets != null) {
                targets.forEach(t -> t.setParentId(component.getId()));
            }
            WPSComponentBean wps = new WPSComponentBean(wpsComponentService.save(component));
            TargetDescriptor target = wps.getTargets().stream().filter(t -> t.getDataDescriptor().getFormatType() == DataFormat.JSON).findFirst().orElse(null);
            if (target != null) {
                final String repoName = "Output of " + wps.getId();
                try {
                    Repository repository = repositoryService.getByUserAndName(currentUser(), repoName);
                    if (repository == null) {
                        repository = JacksonUtil.fromString(target.getDataDescriptor().getLocation(), Repository.class);
                        repository.setName(repoName);
                        repository.setUserName(currentUser());
                        repositoryService.save(repository);
                    }
                } catch (Exception e) {
                    warn("Remote WPS operation output does not represent a valid repository descriptor [%s]", target.getDataDescriptor().getLocation());
                }
            }
            return prepareResult(wps);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody WPSComponentBean entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("Emtpy body");
            }
            if (StringUtilities.isNullOrEmpty(entity.getId())) {
                throw new IllegalArgumentException("Method not intended for new components");
            }
            if (!currentUser().equals(entity.getOwner())) {
                throw new IllegalArgumentException("Entity can only be modified by its owner");
            }
            final Container container = containerService.findById(entity.getServiceId());
            if (container == null) {
                throw new IllegalArgumentException("No WPS service associated");
            }
            final WPSComponent component = entity.toComponent();
            component.setService(container);
            WPSComponent wps = wpsComponentService.update(component);
            TargetDescriptor target = wps.getTargets().stream().filter(t -> t.getDataDescriptor().getFormatType() == DataFormat.JSON).findFirst().orElse(null);
            if (target != null) {
                final String repoName = "Output of " + wps.getId();
                try {
                    Repository repository = repositoryService.getByUserAndName(currentUser(), repoName);
                    if (repository == null) {
                        repository = JacksonUtil.fromString(target.getDataDescriptor().getLocation(), Repository.class);
                        repository.setName(repoName);
                        repository.setUserName(currentUser());
                        repositoryService.save(repository);
                    }
                } catch (Exception e) {
                    warn("Remote WPS operation output does not represent a valid repository descriptor [%s]", target.getDataDescriptor().getLocation());
                }
            }
            return prepareResult(new WPSComponentBean(wps));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("id") String id) {
        try {
            final WPSComponent entity = wpsComponentService.findById(id);
            if (entity == null) {
                throw new Exception("Component '" + id + "' does not exist");
            }
            if (!currentUser().equals(entity.getOwner())) {
                throw new IllegalArgumentException("Entity can only be deleted by its owner");
            }
            wpsComponentService.delete(id);
            return prepareResult("Component deleted", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
