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

/**
 * Abstract base class for controllers containing common functionality for logging,
 * asynchronous task execution, and response preparation.
 * This class includes utility methods to handle exceptions,
 * log messages at different levels, execute tasks asynchronously,
 * and build standardized responses.
 *
 * This class is decorated with a CrossOrigin annotation to allow CORS
 * for specific origins during development or restricted environments.
 */
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

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * This method accesses the {@code SecurityContext} to obtain the
     * {@code Authentication} object, which contains details about the authenticated user.
     * It then extracts and returns the username of the authenticated user.
     *
     * @return the username of the currently authenticated user
     */
    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Executes the given {@link Runnable} asynchronously using a predefined executor service.
     * This overload provides a simplified method for running a task without the need for
     * specifying additional parameters such as success messages or callbacks.
     *
     * @param runnable the {@code Runnable} task to be executed asynchronously
     * @return a {@link Future} representing the result of the asynchronous computation
     */
    protected Future<?> asyncExecute(Runnable runnable) {
        return asyncExecute(runnable, "Operation completed", null);
    }

    /**
     * Handles unhandled exceptions by logging their stack trace.
     *
     * This method is invoked when an unhandled exception occurs. It extracts
     * the stack trace of the given exception and logs it using the internal
     * logger to assist in debugging and error tracking.
     *
     * @param e the {@code Exception} that was unhandled and needs to be logged
     */
    protected void onUnhandledException(Exception e) {
        error(ExceptionUtils.getStackTrace(logger, e));
    }

    /**
     * Executes a {@link Callable} task asynchronously using a predefined executor service.
     *
     * The method provides functionality to handle success and error scenarios
     * through the corresponding callback functions. If the {@code successCallback} is
     * defined, it will be invoked upon the successful execution of the task. If the
     * {@code errorCallback} is defined, it will handle exceptions thrown during the
     * execution of the task. Unhandled exceptions will be managed by the
     * {@code onUnhandledException} method.
     *
     * @param <T>           the type of result produced by the {@code Callable} task
     * @param runnable      the {@code Callable} task to be executed asynchronously
     * @param successCallback a {@code Consumer} function to handle the result produced by the task upon success
     * @param errorCallback a {@code Consumer} function to handle exceptions thrown during task execution
     * @return a {@link Future} representing the result of the asynchronous task, which can be
     *         used to check the task's execution status or retrieve its output
     */
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

    /**
     * Executes a {@link Runnable} task asynchronously using a predefined executor service
     * and allows handling of success and failure scenarios through a callback.
     *
     * This method executes the provided {@code Runnable} task in an asynchronous manner.
     * It invokes the {@code callback} with the username of the current user, any exception
     * thrown during the execution (if applicable), and a message indicating success upon
     * successful execution. If no callback is provided, unhandled exceptions are logged
     * using the {@code onUnhandledException} method.
     *
     * @param runnable       the {@code Runnable} task to be executed asynchronously
     * @param successMessage the success message to be passed to the callback upon successful execution
     * @param callback       a {@link TriConsumer} that accepts the username of the current user,
     *                       the exception (if any occurred during execution), and the success message
     * @return a {@link Future} representing the result of the asynchronous task submission
     */
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

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse} with the given result.
     *
     * @param <T>   the type of the result contained in the {@link ServiceResponse}
     * @param result the result data to be included in the service response
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided result and HTTP status {@code OK}
     */
    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result) {
        return new ResponseEntity<>(new ServiceResponse<>(result),
                                    HttpStatus.OK);
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse} with the specified result
     * and applies cache control headers based on the provided cache timeout.
     *
     * @param <T>                 the type of the result contained in the {@link ServiceResponse}
     * @param result              the result data to be included in the service response
     * @param cacheTimeoutMinutes the cache timeout value in minutes for the response
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided result and HTTP status {@code OK},
     *         including the specified cache control headers
     */
    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, int cacheTimeoutMinutes) {
        CacheControl cacheControl = CacheControl.maxAge(cacheTimeoutMinutes, TimeUnit.MINUTES)
                .noTransform()
                .mustRevalidate();
        return ResponseEntity.ok().cacheControl(cacheControl).body(new ServiceResponse<>(result));
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse} with the specified result
     * and HTTP status.
     *
     * @param <T>       the type of the result contained in the {@link ServiceResponse}
     * @param result    the result data to include in the {@link ServiceResponse}
     * @param httpStatus the HTTP status to be set in the {@link ResponseEntity}
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided result and specified HTTP status
     */
    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(result), httpStatus);
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse}
     * with the given result and message. The response indicates a successful operation
     * with an HTTP status of {@code OK}.
     *
     * @param <T>     the type of the result contained in the {@link ServiceResponse}
     * @param result  the result data to be included in the service response
     * @param message the message to provide additional context about the response
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the
     *         provided result, message, and HTTP status {@code OK}
     */
    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, String message) {
        return new ResponseEntity<>(new ServiceResponse<>(result, message, ResponseStatus.SUCCEEDED), HttpStatus.OK);
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse}
     * with the specified message and status. The response indicates a successful operation
     * with an HTTP status of {@code OK}.
     *
     * @param message the message to provide additional context about the response
     * @param status  the {@link ResponseStatus} indicating the outcome of the operation
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided
     *         message, status, and HTTP status {@code OK}
     */
    protected ResponseEntity<ServiceResponse<?>> prepareResult(String message, ResponseStatus status) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status),
                                    HttpStatus.OK);
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse}
     * with the specified message, response status, and HTTP status.
     *
     * @param message   the message to provide additional context about the response
     * @param status    the {@link ResponseStatus} indicating the outcome of the operation
     * @param httpStatus the HTTP status to be set in the {@link ResponseEntity}
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided
     *         message, response status, and specified HTTP status
     */
    protected ResponseEntity<ServiceResponse<?>> prepareResult(String message, ResponseStatus status, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(message, status), httpStatus);
    }

    /**
     * Prepares a standardized {@link ResponseEntity} containing a {@link ServiceResponse}
     * with the specified result, message, response status, and HTTP status.
     *
     * @param <T>        the type of the result contained in the {@link ServiceResponse}
     * @param result     the result data to be included in the service response
     * @param message    a message providing additional context about the response
     * @param status     the {@link ResponseStatus} indicating the outcome of the operation
     * @param httpStatus the HTTP status to be set in the {@link ResponseEntity}
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the provided
     *         result, message, response status, and specified HTTP status
     */
    protected <T> ResponseEntity<ServiceResponse<?>> prepareResult(T result, String message,
                                                                   ResponseStatus status, HttpStatus httpStatus) {
        return new ResponseEntity<>(new ServiceResponse<>(result, message, status), httpStatus);
    }

    /**
     * Handles exceptions by logging their details and returning an appropriate
     * response entity with a standardized service response.
     *
     * If the exception is an instance of {@code ValidationException}, additional
     * information specific to the exception is included in the response. For all
     * other exceptions, the exception's message is provided in the response.
     *
     * @param ex the exception to be handled
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} with the
     *         details of the failure and an HTTP status of {@code OK}
     */
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

    /**
     * Logs a formatted trace-level message using the provided message template and arguments.
     *
     * If the arguments are provided and not null, the message is formatted using {@code String.format}.
     * Otherwise, the raw message is logged without formatting.
     *
     * @param message the message template to be logged
     * @param args    optional arguments to format the message template, if any
     */
    protected void trace(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.finest(String.format(message, args));
        } else {
            logger.finest(message);
        }
    }

    /**
     * Logs a debug message at the fine level. If arguments are provided, the message is formatted
     * using String.format with the provided arguments.
     *
     * @param message the debug message to log
     * @param args optional arguments to format into the message
     */
    protected void debug(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.fine(String.format(message, args));
        } else {
            logger.fine(message);
        }
    }

    /**
     * Logs an informational message. If additional arguments are provided, the message
     * will be formatted using those arguments.
     *
     * @param message the message to log
     * @param args optional arguments to format the message
     */
    protected void info(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.info(String.format(message, args));
        } else {
            logger.info(message);
        }
    }

    /**
     * Logs a warning message using the specified format and arguments if provided.
     *
     * @param message The warning message to be logged. It may include format specifiers
     *                if additional arguments are supplied.
     * @param args    Optional arguments to be formatted into the message if format
     *                specifiers are present.
     */
    protected void warn(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.warning(String.format(message, args));
        } else {
            logger.warning(message);
        }
    }

    /**
     * Logs an error message using the severe logging level.
     *
     * @param message The error message to be logged. Supports formatting with placeholders if additional arguments are provided.
     * @param args Optional arguments to replace placeholders in the message. If no arguments are provided, the message is logged as is.
     */
    protected void error(String message, Object... args) {
        if (args != null && args.length > 0) {
            logger.severe(String.format(message, args));
        } else {
            logger.severe(message);
        }
    }

    /**
     * Handles exceptions or success scenarios by sending a message to the user via a specified topic.
     *
     * @param userId         the identifier of the user to whom the message is sent
     * @param ex             the exception to be handled; can be null if the operation was successful
     * @param successMessage the message to be sent when the operation is successful; ignored if an exception is provided
     */
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
