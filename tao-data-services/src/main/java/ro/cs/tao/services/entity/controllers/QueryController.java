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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.QueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/datasource/query")
public class QueryController extends BaseController {

    @Autowired
    private QueryService queryService;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getQuery(@RequestParam(name = "userId", required = false) Optional<String> userId,
                                                       @RequestParam(name = "sensor", required = false) Optional<String> sensor,
                                                       @RequestParam(name = "dataSource", required = false) Optional<String> dataSource,
                                                       @RequestParam(name = "nodeId", required = false) Optional<Long> workflowNodeId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (!userId.isPresent() && !sensor.isPresent() && !dataSource.isPresent() && !workflowNodeId.isPresent()) {
                responseEntity = prepareResult(queryService.list());
            } else {
                if (!userId.isPresent()) {
                    responseEntity = prepareResult("Missing userId", ResponseStatus.FAILED);
                } else {
                    if (sensor.isPresent() && dataSource.isPresent() && workflowNodeId.isPresent()) {
                        responseEntity = prepareResult(queryService.getQuery(userId.get(), sensor.get(),
                                                                             dataSource.get(), workflowNodeId.get()));
                    } else if (sensor.isPresent() && dataSource.isPresent()) {
                        responseEntity = prepareResult(queryService.getQueries(userId.get(), sensor.get(), dataSource.get()));
                    } else if (sensor.isPresent()) {
                        responseEntity = prepareResult(queryService.getQueriesBySensor(userId.get(), sensor.get()));
                    } else if (dataSource.isPresent()) {
                        responseEntity = prepareResult(queryService.getQueriesByDataSource(userId.get(), dataSource.get()));
                    } else if (workflowNodeId.isPresent()) {
                        responseEntity = prepareResult(queryService.getQueries(userId.get(), workflowNodeId.get()));
                    } else {
                        responseEntity = prepareResult(queryService.getQueries(userId.get()));
                    }
                }
            }
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/paged", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAllQueries(@RequestParam("page") int pageNumber,
                                                            @RequestParam("pageSize") int size,
                                                            @RequestParam("sort") String sortDirection) {
        Sort.Direction sort = Enum.valueOf(Sort.Direction.class, sortDirection);
        PageRequest page = PageRequest.of(pageNumber, size, new Sort(sort));
        List<Query> queries = new ArrayList<>();
        Page<Query> queryPage = queryService.getAllQueries(page);
        if (queryPage != null) {
            queries.addAll(queryPage.getContent());
        }
        return prepareResult(queries);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> findById(@PathVariable("id") long id) {
        return prepareResult(queryService.getQueryById(id));
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Query object) {
        try {
            object.setUserId(SecurityContextHolder.getContext().getAuthentication().getName());
            return prepareResult(queryService.save(object));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> deleteById(@PathVariable("id") long id) {
        try {
            queryService.delete(id);
            return prepareResult(String.format("Query with id %s deleted", id), ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody Query object) {
        try {
            object.setUserId(SecurityContextHolder.getContext().getAuthentication().getName());
            return prepareResult(queryService.update(object));
        } catch (Exception pex) {
            return handleException(pex);
        }
    }
}
