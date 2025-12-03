package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.PassThroughComponent;
import ro.cs.tao.component.PassThroughComponentBean;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.PassThroughComponentService;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.utils.StringUtilities;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/passthrough")
@Tag(name = "Pass-Through Components", description = "Operations related to pass-through components management")
public class PassThroughComponentController extends BaseController {

    @Autowired
    private PassThroughComponentService componentService;
    @Autowired
    private RepositoryService repositoryService;

    /**
     * Returns a (paged) list of Condition components.
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
            return prepareResult(componentService.list(pageNumber, pageSize, sort).stream().map(PassThroughComponentBean::new).collect(Collectors.toList()));
        } else {
            return prepareResult(componentService.list().stream().map(PassThroughComponentBean::new).collect(Collectors.toList()));
        }
    }

    /**
     * Returns the descriptors of one or more condition components, given their ids.
     * @param idList    The list of identifiers
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(componentService.list(Arrays.asList(ids)).stream()
                                             .map(PassThroughComponentBean::new)
                                             .collect(Collectors.toList()));
    }

    /**
     * Creates a new Condition component.
     *
     * @param entity    The Condition component structure
     */
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody PassThroughComponentBean entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("Emtpy body");
            }
            if (entity.getOwner() == null || entity.getOwner().isEmpty()) {
                entity.setOwner(currentUser());
            }
            final PassThroughComponent component = entity.toComponent();
            final List<SourceDescriptor> sources = component.getSources();
            if (sources != null) {
                sources.forEach(s -> s.setParentId(component.getId()));
            }
            final List<TargetDescriptor> targets = component.getTargets();
            if (targets != null) {
                targets.forEach(t -> t.setParentId(component.getId()));
            }
            return prepareResult(new PassThroughComponentBean(componentService.save(component)));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Updates an existing Condition component.
     *
     * @param entity    The Condition component structure
     */
    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody PassThroughComponentBean entity) {
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
            PassThroughComponent component = entity.toComponent();
            component = componentService.update(component);
            return prepareResult(new PassThroughComponentBean(component));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Removes a Condition component.
     *
     * @param id    The Condition component identifier
     */
    @RequestMapping(value = "/", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("id") String id) {
        try {
            final PassThroughComponent entity = componentService.findById(id);
            if (entity == null) {
                throw new Exception("Component '" + id + "' does not exist");
            }
            if (!currentUser().equals(entity.getOwner())) {
                throw new IllegalArgumentException("Entity can only be deleted by its owner");
            }
            componentService.delete(id);
            return prepareResult("Component deleted", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
