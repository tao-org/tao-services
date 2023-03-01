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

import org.reflections.Reflections;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class Endpoints {
    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final String REFRESH_ENDPOINT = "/auth/refresh";
    public static final String LOGIN_ENDPOINT_METHOD = "POST";
    public static final String DOWNLOAD_ENDPOINT = "/files/get";
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
    private static Set<String> endpointList;

    public static Set<String> endpoints(String packagePrefix) {
        if (endpointList == null) {
            endpointList = new HashSet<>();
            Set<Class<?>> controllers = new Reflections(packagePrefix).getTypesAnnotatedWith(RestController.class);
            for (Class<?> controller : controllers) {
                String path;
                RequestMapping annotation = controller.getAnnotation(RequestMapping.class);
                if (annotation != null && annotation.value().length > 0) {
                    path = annotation.value()[0];
                    if (!"/user".equals(path)) {
                        Method[] methods = controller.getDeclaredMethods();
                        for (Method method : methods) {
                            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                            if (mapping != null && mapping.value().length > 0) {
                                path += mapping.value()[0];
                            } else {
                                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                                if (getMapping != null && getMapping.value().length > 0) {
                                    path += getMapping.value()[0];
                                } else {
                                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                                    if (postMapping != null && postMapping.value().length > 0) {
                                        path += postMapping.value()[0];
                                    } else {
                                        PutMapping putMapping = method.getAnnotation(PutMapping.class);
                                        if (putMapping != null && putMapping.value().length > 0) {
                                            path += putMapping.value()[0];
                                        } else {
                                            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                                            if (deleteMapping != null && deleteMapping.value().length > 0) {
                                                path += deleteMapping.value()[0];
                                            }
                                        }
                                    }
                                }
                            }
                            endpointList.add(generalizePath(path));
                        }
                    } else {
                        // "/user/activate/{username}", "/user/reset/{username}"  should remain free (accessed from activation email)
                        endpointList.add(path + "/*");
                    }

                }
            }
            endpointList.remove(REFRESH_ENDPOINT);
        }
        return endpointList;
    }

    private static String generalizePath(String path) {
        StringBuilder newPath = new StringBuilder();
        int slashCount = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                slashCount++;
            }
        }
        if (slashCount == 1) {
            newPath.append(path);
        } else if (slashCount >= 2) {
            newPath.append(path.substring(0, path.indexOf('/', 1) + 1)).append("*");
        }
        return newPath.toString();
    }
}
