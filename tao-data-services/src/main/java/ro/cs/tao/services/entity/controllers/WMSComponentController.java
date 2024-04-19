package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.WMSComponentBean;
import ro.cs.tao.component.ogc.WMSComponent;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.interfaces.WMSComponentService;
import ro.cs.tao.utils.JacksonUtil;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wmscomponent")
@Tag(name = "WMS Components", description = "Operations related to WMS components managements")
public class WMSComponentController extends BaseController {

    @Autowired
    private ContainerService containerService;
    @Autowired
    private WMSComponentService wmsComponentService;
    @Autowired
    private RepositoryService repositoryService;

    /**
     * Returns a (paged) list of WMS components.
     * The optional parameters are either all set or none is set.
     *
     * @param pageNumber    (optional) The page number
     * @param pageSize      (optional) Items per page
     * @param sortByField   (optional) The sort field
     * @param sortDirection (optional) The sort direction (ASC or DESC)
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(wmsComponentService.list(pageNumber, pageSize, sort).stream().map(WMSComponentBean::new).collect(Collectors.toList()));
        } else {
            return prepareResult(wmsComponentService.list().stream().map(WMSComponentBean::new).collect(Collectors.toList()));
        }
    }

    /**
     * Creates a new WMS component.
     *
     * @param entity    The WMS component structure
     */
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody WMSComponentBean entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("Emtpy body");
            }
            if (entity.getOwner() == null || entity.getOwner().isEmpty()) {
                entity.setOwner(currentUser());
            }
            final Container container = containerService.findById(entity.getServiceId());
            if (container == null) {
                throw new IllegalArgumentException("No WMS service associated");
            }
            final WMSComponent component = entity.toComponent();
            component.setService(container);
            final List<SourceDescriptor> sources = component.getSources();
            if (sources != null) {
                sources.forEach(s -> s.setParentId(component.getId()));
            }
            final List<TargetDescriptor> targets = component.getTargets();
            if (targets != null) {
                targets.forEach(t -> t.setParentId(component.getId()));
            }
            WMSComponentBean wps = new WMSComponentBean(wmsComponentService.save(component));
            TargetDescriptor target = wps.getTargets().stream().filter(t -> t.getDataDescriptor().getFormatType() == DataFormat.JSON).findFirst().orElse(null);
            if (target != null) {
                final String repoName = "Output of " + wps.getId();
                try {
                    Repository repository = repositoryService.getByUserAndName(currentUser(), repoName);
                    if (repository == null) {
                        repository = JacksonUtil.fromString(target.getDataDescriptor().getLocation(), Repository.class);
                        repository.setName(repoName);
                        repository.setUserId(currentUser());
                        repositoryService.save(repository);
                    }
                } catch (Exception e) {
                    warn("Remote WMS operation output does not represent a valid repository descriptor [%s]", target.getDataDescriptor().getLocation());
                }
            }
            return prepareResult(wps);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Updates an existing WMS component.
     *
     * @param entity    The WMS component structure
     */
    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody WMSComponentBean entity) {
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
                throw new IllegalArgumentException("No WMS service associated");
            }
            final WMSComponent component = entity.toComponent();
            component.setService(container);
            WMSComponent wps = wmsComponentService.update(component);
            TargetDescriptor target = wps.getTargets().stream().filter(t -> t.getDataDescriptor().getFormatType() == DataFormat.JSON).findFirst().orElse(null);
            if (target != null) {
                final String repoName = "Output of " + wps.getId();
                try {
                    Repository repository = repositoryService.getByUserAndName(currentUser(), repoName);
                    if (repository == null) {
                        repository = JacksonUtil.fromString(target.getDataDescriptor().getLocation(), Repository.class);
                        repository.setName(repoName);
                        repository.setUserId(currentUser());
                        repositoryService.save(repository);
                    }
                } catch (Exception e) {
                    warn("Remote WMS operation output does not represent a valid repository descriptor [%s]", target.getDataDescriptor().getLocation());
                }
            }
            return prepareResult(new WMSComponentBean(wps));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Removes a WMS component.
     *
     * @param id    The WMS component identifier
     */
    @RequestMapping(value = "/", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("id") String id) {
        try {
            final WMSComponent entity = wmsComponentService.findById(id);
            if (entity == null) {
                throw new Exception("Component '" + id + "' does not exist");
            }
            if (!currentUser().equals(entity.getOwner())) {
                throw new IllegalArgumentException("Entity can only be deleted by its owner");
            }
            wmsComponentService.delete(id);
            return prepareResult("Component deleted", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
