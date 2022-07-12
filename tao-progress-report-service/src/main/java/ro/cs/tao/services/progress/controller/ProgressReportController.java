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
package ro.cs.tao.services.progress.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.datasource.DownloadManager;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ProgressReportService;
import ro.cs.tao.services.progress.impl.Filter;
import ro.cs.tao.utils.StringUtilities;

import java.util.Base64;

@Controller
@RequestMapping("/progress")
public class ProgressReportController extends ControllerBase {

    @Autowired
    private ProgressReportService progressReportService;

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getTasksInProgress(@RequestParam(name = "category", required = false) String category,
                                                                 @RequestParam(name = "filter", required = false) String jsonFilter) {
        final String filter;
        if (StringUtilities.isNullOrEmpty(jsonFilter)) {
            Filter f = new Filter();
            f.setName("Principal");
            f.setValue(currentUser());
            filter = f.toString();
        } else {
            filter = new String(Base64.getDecoder().decode(jsonFilter));
        }
        return prepareResult(progressReportService.getRunningTasks(category, filter));
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getDownloadStatus() {
        return prepareResult(DownloadManager.getCurrentStatus());
    }

}
