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
package ro.cs.tao.services.commons;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.user.Group;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:63343"})
public class BaseController {

    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final String LOGIN_ENDPOINT_METHOD = "POST";
    public static final String LOGOUT_ENDPOINT = "/auth/logout";

    public static final String ADMIN_SERVICE_PATH_EXPRESSION = "/admin/**/*";
    //public static final String USER_SERVICE_PATH_EXPRESSION = "/user/**/*";
    public static final String USER_SERVICE_PATH_EXPRESSION = "/user/*"; //  "/user/activate/{username}" should remain free (accessed from activation email)
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
    protected static PersistenceManager persistenceManager;

    private ExecutorService executorService;

    protected Logger logger = Logger.getLogger(getClass().getName());

    public static void setPersistenceManager(PersistenceManager persistenceManager) {
        BaseController.persistenceManager = persistenceManager;
    }

    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    protected void asyncExecute(Runnable runnable, Consumer<Exception> callback) {
        if (this.executorService == null) {
            executorService = new NamedThreadPoolExecutor("controller-async", 1);//Executors.newSingleThreadExecutor();
        }

        executorService.submit(() -> {
            Exception exception = null;
            try {
                runnable.run();
            } catch (Exception ex) {
                exception = ex;
            } finally {
                if (callback != null) {
                    callback.accept(exception);
                }
            }
        });
    }

    protected boolean isCurrentUserAdmin() {
        List<Group> groups = persistenceManager.getUserGroups(SessionStore.currentContext().getPrincipal().getName());
        return (groups != null && groups.stream().anyMatch(g -> "ADMIN".equals(g.getName())));
    }

    protected <T> ResponseEntity<ServiceResponse<T>> prepareResult(T result) {
        return new ResponseEntity<>(new ServiceResponse<>(result), HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse> prepareResult(String message, ResponseStatus status) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status), HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse<?>> handleException(Exception ex) {
        Logger.getLogger(getClass().getName()).severe(ex.getMessage());
        return new ResponseEntity<>(new ServiceResponse<>(String.format("Failed with error: %s",
                                                                        ex.getMessage()),
                                                          ResponseStatus.FAILED),
                                    HttpStatus.OK);
    }
}
