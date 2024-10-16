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
package ro.cs.tao.services.cleanup.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.RoleRequired;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.CleanupService;

/**
 * @author Lucian Barbulescu
 */
@RestController
@RequestMapping("/cleanup")
@Tag(name ="Cleanup", description = "Endpoint for removing the products with no valid files attached from the database")
public class CleanupController extends BaseController {

	/**	Cleanup service. */
	@Autowired
	private CleanupService cleanupService;
	
    /**
     * Perform the cleanup operation.
     */
    @RequestMapping(value = "/database/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> cleanupDatabase() {
//        if (isCurrentUserAdmin()) {
        	final long deletedProducts = cleanupService.cleanupDatabase();
        	
            return prepareResult("Finished successfully. Number of deleted products: " + deletedProducts, ResponseStatus.SUCCEEDED, HttpStatus.OK);
//        } else {
//            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
//        }
    }
}
