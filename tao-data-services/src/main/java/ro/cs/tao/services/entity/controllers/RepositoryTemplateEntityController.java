package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Repositories", description = "Operations related to public repository management")
public class RepositoryTemplateEntityController extends DataEntityController<RepositoryTemplate, String, RepositoryTemplateService> {

    /**
     * Handles listing entities with optional pagination and sorting parameters.
     *
     * @param pageNumber the optional page number for paginated results
     * @param pageSize the optional page size for paginated results
     * @param sortByField the optional field name to sort the results by
     * @param sortDirection the optional direction of sorting (ascending or descending)
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} with the list of entities,
     *         either in paginated or unpaginated format, and the corresponding HTTP status
     */
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

    /**
     * Retrieves a repository entity by its identifier.
     *
     * @param id the unique identifier of the repository entity to retrieve
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} with the retrieved entity data,
     *         or an error response if the entity cannot be found or an exception occurs
     */
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

    /**
     * Saves the provided repository template entity to the system. The method
     * delegates to the superclass save implementation for primary processing
     * and handles any exceptions encountered during the operation.
     *
     * @param entity the {@link RepositoryTemplate} entity to be saved
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse}
     *         with the outcome of the save operation, or an error response
     *         if an exception occurs
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody RepositoryTemplate entity) {
        try {
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Updates an existing repository template entity identified by the given ID.
     * Delegates the update operation to the superclass implementation and handles
     * any exceptions that occur during the process.
     *
     * @param id the unique identifier of the repository template entity to update
     * @param entity the {@link RepositoryTemplate} entity containing updated data
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} with
     *         the outcome of the update operation, or an error response if an
     *         exception occurs
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody RepositoryTemplate entity) {
        try {
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
