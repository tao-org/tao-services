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

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ProductService;

import java.io.IOException;
import java.nio.file.Paths;

@Controller
@RequestMapping("/product")
public class ProductController extends DataEntityController<EOProduct, String, ProductService> {

    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> inspect(@RequestParam("sourceDir") String sourceDir) {
        if (sourceDir == null) {
            return prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        try {
            return prepareResult(this.service.inspect(Paths.get(sourceDir)));
        } catch (IOException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> importProducts(@RequestParam("sourceDir") String sourceDir,
                                                             @RequestParam("linkOnly") boolean linkOnly) {
        if (sourceDir == null) {
            return prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        try {
            return prepareResult(this.service.importProducts(sourceDir, linkOnly));
        } catch (IOException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/check", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> checkExistingProducts(@RequestParam("names") String[] names) {
        return prepareResult(this.service.checkExisting(names));
    }
}
