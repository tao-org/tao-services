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

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.TriConsumer;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

@CrossOrigin(origins = {"http://localhost", "http://localhost:8080", "http://localhost:8081", "http://localhost:8082"})
public abstract class ControllerBase {

    private static final ThreadPoolTaskExecutor executorService;

    static {
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
        executorService.setCorePoolSize(Math.min(processors / 2, 4));
        executorService.setMaxPoolSize(executorService.getCorePoolSize() * 4);
        executorService.setQueueCapacity(25);
        executorService.initialize();
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    protected Future<?> asyncExecute(Runnable runnable) {
        return asyncExecute(runnable, "Operation completed", null);
    }

    protected void onUnhandledException(Exception e) {
        error(ExceptionUtils.getStackTrace(logger, e));
    }

    protected <T> Future<?> asyncExecute(Callable<T> runnable, Consumer<T> successCallback, Consumer<Exception> errorCallback) {
        return executorService.submit(() -> {
            try {
                final T result = runnable.call();
                if (successCallback != null) {
                    successCallback.accept(result);
                }
            } catch (Exception ex) {
                if (errorCallback != null) {
                    errorCallback.accept(ex);
                } else {
                    onUnhandledException(ex);
                }
            }
        });
    }

    protected Future<?> asyncExecute(Runnable runnable, String successMessage, TriConsumer<String, Exception, String> callback) {
        final String user = currentUser();
        return executorService.submit(() -> {
            try {
                runnable.run();
                if (callback != null) {
                    callback.accept(user, null, successMessage);
                }
            } catch (Exception ex) {
                if (callback != null) {
                    callback.accept(user, ex, null);
                } else {
                    onUnhandledException(ex);
                }
            }
        });
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result) {
        return new ResponseEntity<>(new ServiceResponse<>(result),
                                    HttpStatus.OK);
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, int cacheTimeoutMinutes) {
        CacheControl cacheControl = CacheControl.maxAge(cacheTimeoutMinutes, TimeUnit.MINUTES)
                .noTransform()
                .mustRevalidate();
        return ResponseEntity.ok().cacheControl(cacheControl).body(new ServiceResponse<>(result));
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(result), httpStatus);
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, String message) {
        return new ResponseEntity<>(new ServiceResponse<>(result, message, ResponseStatus.SUCCEEDED), HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse<?>> prepareResult(String message, ResponseStatus status) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status),
                                    HttpStatus.OK);
    }

    protected ResponseEntity<ServiceResponse<?>> prepareResult(String message, ResponseStatus status, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status), httpStatus);
    }

    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, String message,
                                                                   ResponseStatus status, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(result, message, status), httpStatus);
    }

    protected ResponseEntity<ServiceResponse<?>> handleException(Exception ex) {
        logger.severe("Exception in session of " + currentUser() + ":");
        logger.severe(ExceptionUtils.getStackTrace(logger, ex));
        if (ex instanceof ValidationException) {
            ValidationException vex = (ValidationException) ex;
            return new ResponseEntity<>(new ServiceResponse<>(vex.getAdditionalInfo(),
                                                              ResponseStatus.FAILED),
                                        HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ServiceResponse<>(ex.getMessage(),
                                                              ResponseStatus.FAILED),
                                        HttpStatus.OK);
        	
        }
    }

    protected void trace(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.finest(String.format(message, args));
        } else {
            logger.finest(message);
        }
    }

    protected void debug(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.fine(String.format(message, args));
        } else {
            logger.fine(message);
        }
    }

    protected void info(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.info(String.format(message, args));
        } else {
            logger.info(message);
        }
    }

    protected void warn(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.warning(String.format(message, args));
        } else {
            logger.warning(message);
        }
    }

    protected void error(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.severe(String.format(message, args));
        } else {
            logger.severe(message);
        }
    }

    protected void exceptionCallbackHandler(String userId, Exception ex, String successMessage) {
        final Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        String msg;
        final String topic;
        if (ex != null) {
            msg = ex.getMessage();
            message.setData(msg);
            topic = Topic.ERROR.value();
            logger.severe(msg);
        } else {
            msg = successMessage;
            message.setData(msg);
            topic = Topic.INFORMATION.value();
            logger.info(msg);
        }
        Messaging.send(userId, topic, message);
    }
}
