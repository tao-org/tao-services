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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.datasource.DownloadManager;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ProgressReportService;
import ro.cs.tao.services.progress.impl.Filter;
import ro.cs.tao.utils.StringUtilities;

import java.util.Base64;

@RestController
@RequestMapping("/progress")
@Tag(name = "Progress", description = "Reports the progress of lengthy operations")
public class ProgressReportController extends BaseController {

    @Autowired
    private ProgressReportService progressReportService;

    /**
     * Returns details about running tasks.
     * @param category  The task category
     * @param userId  The user account name
     * @param jsonFilter    Additional filter
     */
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTasksInProgress(@RequestParam(name = "category", required = false) String category,
                                                                 @RequestParam(name = "userId", required = false) String userId,
                                                                 @RequestParam(name = "filter", required = false) String jsonFilter) {
        String filter = null;
        if (!StringUtilities.isNullOrEmpty(userId) && (currentUser().equals(userId) || isCurrentUserAdmin())) {
            if (StringUtilities.isNullOrEmpty(jsonFilter)) {
                Filter f = new Filter();
                f.setName("Principal");
                f.setValue(userId);
                filter = f.toString();
            } else {
                filter = new String(Base64.getDecoder().decode(jsonFilter));
            }
        } else if (StringUtilities.isNullOrEmpty(userId)) {
            if (StringUtilities.isNullOrEmpty(jsonFilter)) {
                Filter f = new Filter();
                f.setName("Principal");
                f.setValue(currentUser());
                filter = f.toString();
            } else {
                filter = new String(Base64.getDecoder().decode(jsonFilter));
            }
        }
        return prepareResult(progressReportService.getRunningTasks(category, filter));
    }

    /**
     * Returns statistics about downloads
     */
    @RequestMapping(value = "/download/statistics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getGlobalDownloadStatus() {
        return prepareResult(DownloadManager.getOverallStatus());
    }
    /**
     * Returns information about the current downloads
     */
    @RequestMapping(value = "/download/statistics/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getDownloadStatus() {
        return prepareResult(DownloadManager.getCurrentStatus());
    }

}
