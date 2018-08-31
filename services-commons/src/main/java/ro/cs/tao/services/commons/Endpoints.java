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

package ro.cs.tao.services.commons;

public class Endpoints {
    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final String LOGIN_ENDPOINT_METHOD = "POST";
    public static final String LOGOUT_ENDPOINT = "/auth/logout";
    public static final String ADMIN_SERVICE_PATH_EXPRESSION = "/admin/**/*";
    //public static final String USER_SERVICE_PATH_EXPRESSION = "/user/**/*";
    public static final String USER_SERVICE_PATH_EXPRESSION = "/user/*"; //  "/user/activate/{username}", "/user/reset/{username}"  should remain free (accessed from activation email)
    public static final String DATA_QUERY_SERVICE_PATH_EXPRESSION = "/query/*";
    public static final String MONITORING_SERVICE_PATH_EXPRESSION = "/monitor/*";
    public static final String COMPONENT_SERVICE_PATH_EXPRESSION = "/component/*";
    public static final String CONFIGURATION_SERVICE_PATH_EXPRESSION = "/config";
    public static final String CONTAINER_SERVICE_PATH_EXPRESSION = "/docker/*";
    public static final String DATA_SOURCE_COMPONENT_SERVICE_PATH_EXPRESSION = "/datasource/*";
    public static final String FILES_SERVICE_PATH_EXPRESSION = "/files/**/*";
    public static final String PRODUCT_SERVICE_PATH_EXPRESSION = "/product/*";
    public static final String QUERY_SERVICE_PATH_EXPRESSION = "/datasource/query/*";
    public static final String TOPOLOGY_SERVICE_PATH_EXPRESSION = "/topology/*";
    public static final String WORKFLOW_SERVICE_PATH_EXPRESSION = "/workflow/**/*";
    public static final String ORCHESTRATOR_SERVICE_PATH_EXPRESSION = "/orchestrator/**/*";
    public static final String API_PATH_EXPRESSION = "/api/**/*";
    public static final String GLOBAL_PATH_EXPRESSION = "/**/*";
}
