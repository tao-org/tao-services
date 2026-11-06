/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.SortDirection;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.QueryService;

import java.util.List;
import java.util.Optional;

/**
 * Controller managing operations related to saved queries.
 *
 * This controller provides endpoints to create, retrieve, update,
 * and delete saved queries. It also supports filtering and pagination
 * functionalities to manage queries effectively.
 *
 * The controller is annotated with {@code @RestController} and
 * {@code @RequestMapping} to provide RESTful web services for managing
 * queries within a data source.
 */
@RestController
@RequestMapping({ "/datasource/query", "/datasource/query/" })
@Tag(name = "Saved Queries", description = "Operations related to the management of saved queries")
public class QueryController extends BaseController {

    @Autowired
    private QueryService queryService;

    @Autowired
    private DataSourceComponentService dataSourceComponentService;

    /**
     * Returns a list of saved queries according to the given filters.
     * @param sensor    The sensor (or collection) name
     * @param dataSource    The data source name
     * @param workflowNodeId    The workflow node identifier
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getQuery(@RequestParam(name = "sensor", required = false) Optional<String> sensor,
                                                       @RequestParam(name = "dataSource", required = false) Optional<String> dataSource,
                                                       @RequestParam(name = "nodeId", required = false) Optional<Long> workflowNodeId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (sensor.isEmpty() && dataSource.isEmpty() && workflowNodeId.isEmpty()) {
                responseEntity = prepareResult(queryService.list());
            } else {
                final String userId = currentOwner();
                if (sensor.isPresent() && dataSource.isPresent() && workflowNodeId.isPresent()) {
                    responseEntity = prepareResult(queryService.getQuery(userId, sensor.get(),
                                                                         dataSource.get(), workflowNodeId.get()));
                } else if (sensor.isPresent() && dataSource.isPresent()) {
                    responseEntity = prepareResult(queryService.getQueries(userId, sensor.get(), dataSource.get()));
                } else if (sensor.isPresent()) {
                    responseEntity = prepareResult(queryService.getQueriesBySensor(userId, sensor.get()));
                } else if (dataSource.isPresent()) {
                    responseEntity = prepareResult(queryService.getQueriesByDataSource(userId, dataSource.get()));
                } else {
                    responseEntity = prepareResult(queryService.getQueries(userId, workflowNodeId.get()));
                }
            }
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Retrieves a paginated and sorted list of queries.
     *
     * @param pageNumber    The page number to retrieve. It must be a non-negative integer.
     * @param size          The number of items per page. It must be a positive integer.
     * @param sortDirection The direction of sorting, either "ASC" (ascending) or "DESC" (descending).
     * @return              A {@code ResponseEntity} containing a {@code ServiceResponse} wrapping the list of queries.
     */
    @RequestMapping(value = { "/paged", "/paged/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAllQueries(@RequestParam("page") int pageNumber,
                                                            @RequestParam("pageSize") int size,
                                                            @RequestParam("sort") String sortDirection) {
        SortDirection sort = Enum.valueOf(SortDirection.class, sortDirection);
        List<Query> queries = queryService.getQueries(pageNumber, size, ro.cs.tao.Sort.by("id", sort));
        return prepareResult(queries);
    }

    /**
     * Retrieves a query by its unique identifier.
     *
     * @param id The unique identifier of the query to be retrieved.
     * @return A {@code ResponseEntity} containing a {@code ServiceResponse} with the query data if found,
     *         or an appropriate error message if the query does not exist.
     */
    @RequestMapping(value = { "/{id}", "/{id}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> findById(@PathVariable("id") long id) {
        return prepareResult(queryService.getQueryById(id));
    }

    /**
     * Handles the creation and saving of a new {@code Query} object.
     *
     * @param object The {@code Query} object to be saved. It is expected to be provided in the request body
     *               as a JSON object.
     * @return A {@code ResponseEntity} containing a {@code ServiceResponse} with the result of the save operation.
     *         If successful, the result will include the saved query object. If an error occurs, an appropriate
     *         error message and status will be returned.
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Query object) {
        try {
            object.setUserId(currentOwner());
            final List<DataSourceComponent> sources = dataSourceComponentService.getBySourceAndSensor(object.getDataSource(), object.getSensor());
            if (sources != null && !sources.isEmpty()) {
                DataSourceComponent component = sources.get(0);
                object.setComponentId(component.getId());
            }
            return prepareResult(queryService.save(object));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Deletes a query by its ID.
     * @param id The ID of the query to delete.
     * @return A {@code ResponseEntity} containing a {@code ServiceResponse} with the result of the delete operation.
     */
    @RequestMapping(value = { "/{id}", "/{id}/" }, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> deleteById(@PathVariable("id") long id) {
        try {
            queryService.delete(id);
            return prepareResult(String.format("Query with id %s deleted", id), ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    /**
     * Updates a query.
     * @param object The query object to update.
     * @return A {@code ResponseEntity} containing a {@code ServiceResponse} with the result of the update operation.
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody Query object) {
        try {
            object.setUserId(currentOwner());
            if (object.getComponentId() == null) {
                final List<DataSourceComponent> sources = dataSourceComponentService.getBySourceAndSensor(object.getDataSource(), object.getSensor());
                if (sources != null && !sources.isEmpty()) {
                    DataSourceComponent component = sources.get(0);
                    object.setComponentId(component.getId());
                }
            }
            return prepareResult(queryService.update(object));
        } catch (Exception pex) {
            return handleException(pex);
        }
    }
}
