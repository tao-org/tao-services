package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.RepositoryTemplateService;
import ro.cs.tao.workspaces.RepositoryTemplate;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/repository")
public class RepositoryTemplateEntityController extends DataEntityController<RepositoryTemplate, String, RepositoryTemplateService> {

    @Override
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        try {
            if (!(pageNumber.isPresent() || pageSize.isPresent() || sortByField.isPresent() || sortDirection.isPresent())) {
                return prepareResult(this.service.list().stream().map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
            } else{
                return prepareResult(this.service.list(pageNumber,
                                                       pageSize,
                                                       Sort.by(sortByField.orElse("name"),
                                                               sortDirection.orElse(SortDirection.ASC))).stream()
                                                 .map(ServiceTransformUtils::toBean).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        try {
            final RepositoryTemplate entity = this.service.findById(id);
            if (entity == null) {
                throw new IllegalArgumentException("You cannot access this repository");
            }
            return prepareResult(ServiceTransformUtils.toBean(entity));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody RepositoryTemplate entity) {
        try {
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody RepositoryTemplate entity) {
        try {
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
