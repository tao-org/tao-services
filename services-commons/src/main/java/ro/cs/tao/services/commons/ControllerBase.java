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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.function.Consumer;
import java.util.logging.Logger;

@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:63343"})
public abstract class ControllerBase {
    private static final Object sharedLock = new Object();

    private ThreadPoolTaskExecutor executorService;

    private Logger logger = Logger.getLogger(getClass().getName());

    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    protected void asyncExecute(Runnable runnable) {
        asyncExecute(runnable, null);
    }

    protected void asyncExecute(Runnable runnable, Consumer<Exception> callback) {
        synchronized (sharedLock) {
            if (this.executorService == null) {
                executorService = new ThreadPoolTaskExecutor() {
                    @Override
                    public void execute(Runnable task) {
                        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        super.execute(() -> {
                            try {
                                SecurityContext context = SecurityContextHolder.createEmptyContext();
                                context.setAuthentication(authentication);
                                SecurityContextHolder.setContext(context);
                                task.run();
                            } finally {
                                SecurityContextHolder.clearContext();
                            }
                        });
                    }
                };
                executorService.setThreadGroupName("controller-async");
                int processors = Runtime.getRuntime().availableProcessors();
                executorService.setCorePoolSize(Math.min(processors / 2, 2));
                executorService.setMaxPoolSize(processors * 2);
                executorService.setQueueCapacity(25);
                executorService.initialize();
            }
        }
        executorService.execute(() -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                if (callback != null) {
                    callback.accept(ex);
                } else {
                    onUnhandledException(ex);
                }
            }
        });
    }

    protected void onUnhandledException(Exception e) {
        error(ExceptionUtils.getStackTrace(e));
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result) {
        return new ResponseEntity<>(new ServiceResponse<>(result),
                                    HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse<?>> prepareResult(String message, ResponseStatus status) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status),
                                    HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse<?>> handleException(Exception ex) {
        Logger.getLogger(getClass().getName()).severe(ex.getMessage());
        return new ResponseEntity<>(new ServiceResponse<>(String.format("Failed with error: %s",
                                                                        ex.getMessage()),
                                                          ResponseStatus.FAILED),
                                    HttpStatus.OK);
    }

    protected void debug(String message, Object... args) {
        logger.fine(String.format(message, args));
    }

    protected void info(String message, Object... args) {
        logger.info(String.format(message, args));
    }

    protected void warn(String message, Object... args) {
        logger.warning(String.format(message, args));
    }

    protected void error(String message, Object... args) {
        logger.severe(String.format(message, args));
    }
}
