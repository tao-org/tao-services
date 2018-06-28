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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author Cosmin Cara
 */
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:63343"})
public class BaseController {

    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final String LOGIN_ENDPOINT_METHOD = "POST";
    public static final String LOGOUT_ENDPOINT = "/auth/logout";

    public static final String ADMIN_SERVICE_PATH_EXPRESSION = "/admin/**/*";

    public static final String API_PATH_EXPRESSION = "/api/**/*";
    public static final String GLOBAL_PATH_EXPRESSION = "/**/*";

    private ExecutorService executorService;

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
}
