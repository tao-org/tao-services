/*
 * Copyright (C) 2017 CS ROMANIA
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.QueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/datasource/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getQuery(@RequestParam(name = "userId", required = false) Optional<String> userId,
                                      @RequestParam(name = "sensor", required = false) Optional<String> sensor,
                                      @RequestParam(name = "dataSource", required = false) Optional<String> dataSource,
                                      @RequestParam(name = "nodeId", required = false) Optional<Long> workflowNodeId) {
        ResponseEntity<?> responseEntity;
        try {
            if (!userId.isPresent() && !sensor.isPresent() && !dataSource.isPresent() && !workflowNodeId.isPresent()) {
                responseEntity = new ResponseEntity<>(queryService.list(), HttpStatus.OK);
            } else {
                if (!userId.isPresent()) {
                    responseEntity = new ResponseEntity<>(new ServiceError("Missing userId"), HttpStatus.OK);
                } else {
                    if (sensor.isPresent()) {
                        responseEntity =
                                dataSource.map(s -> new ResponseEntity<>(queryService.getQuery(userId.get(),
                                                                                               sensor.get(),
                                                                                               s),
                                                                         HttpStatus.OK))
                                        .orElseGet(() -> new ResponseEntity<>(queryService.getQueriesBySensor(userId.get(),
                                                                                                              sensor.get()),
                                                                              HttpStatus.OK));
                    } else {
                        responseEntity =
                                dataSource.map(s -> new ResponseEntity<>(queryService.getQueriesByDataSource(userId.get(),
                                                                                                             s),
                                                                                  HttpStatus.OK))
                                        .orElseGet(() -> new ResponseEntity<>(queryService.getQueries(userId.get()),
                                                                              HttpStatus.OK));
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/paged", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getAllQueries(@RequestParam("page") int pageNumber,
                                           @RequestParam("pageSize") int size,
                                           @RequestParam("sort") String sortDirection) {
        Sort.Direction sort = Enum.valueOf(Sort.Direction.class, sortDirection);
        PageRequest page = new PageRequest(pageNumber, size, new Sort(sort));
        List<Query> queries = new ArrayList<>();
        Page<Query> queryPage = queryService.getAllQueries(page);
        if (queryPage != null) {
            queries.addAll(queryPage.getContent());
        }
        return new ResponseEntity<>(queries, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public Query findById(@PathVariable("id") long id) throws PersistenceException {
        return queryService.getQueryById(id);
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    public Query save(@RequestBody Query object) {
        return queryService.save(object);
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT, produces = "application/json")
    public Query update(@RequestBody Query object) {
        return queryService.update(object);
    }
}
