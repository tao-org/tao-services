package ro.cs.tao.services.entity.controllers;

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
public class SiteController extends DataEntityController<Site, String, SiteService> {

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
            return prepareResult(this.service.list(pageNumber, pageSize, Sort.by(sortByField.get(), sortDirection.get())));
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        try {
            final Site entity = this.service.findById(id);
            if (entity == null || !entity.getUser().equals(currentUser())) {
                throw new IllegalArgumentException("You cannot access this object");
            }
            return prepareResult(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Site entity) {
        try {
            entity.setUser(currentUser());
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody Site entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUser()) ||
                    !entity.getUser().equals(currentUser())) {
                throw new IllegalArgumentException("Cannot update this object");
            }
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}