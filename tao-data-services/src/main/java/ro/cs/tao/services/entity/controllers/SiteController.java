package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.SiteService;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Site;

import java.util.Optional;

@RestController
@RequestMapping("/site")
@Tag(name = "Sites", description = "Operations related to sites management")
public class SiteController extends DataEntityController<Site, String, SiteService> {

    /**
     * Returns a (paged) list of sites.
     * The optional parameters are either all given or none is set.
     *
     * @param pageNumber    (optional) The page number
     * @param pageSize      (optional) Items per page
     * @param sortByField   (optional) The sort field
     * @param sortDirection (optional) The sort direction (ASC or DESC)
     */
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
        } else {
            return prepareResult(this.service.list(pageNumber, pageSize, Sort.by(sortByField.orElse("id"), sortDirection.orElse(SortDirection.ASC))));
        }
    }

    /**
     * Returns the definition of a site.
     * Only an administrator or the owner of the site can retrieve the information.
     *
     * @param id    The site identifier
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        try {
            final Site entity = this.service.findById(id);
            if (entity == null || !entity.getUserId().equals(currentUser())) {
                throw new IllegalArgumentException("You cannot access this object");
            }
            return prepareResult(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Creates a new site definition.
     *
     * @param entity    The site definition structure
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Site entity) {
        try {
            entity.setUserId(currentUser());
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Updates an existing site definition.
     *
     * @param id    The site identifier
     * @param entity    The site definition structure
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody Site entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserId()) ||
                    !entity.getUserId().equals(currentUser())) {
                throw new IllegalArgumentException("Cannot update this object");
            }
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}