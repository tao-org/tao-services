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
package ro.cs.tao.services.monitoring.impl;

import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
import ro.cs.tao.execution.model.ProcessingExecutionTask;
import ro.cs.tao.execution.persistence.ExecutionTaskProvider;
import ro.cs.tao.persistence.NodeDBProvider;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.user.Role;
import ro.cs.tao.user.User;
import ro.cs.tao.utils.executors.OutputConsumer;
import ro.cs.tao.utils.executors.SSHExecutor;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * WebSocket handler that streams the live output (stdout and stderr) of a remotely executing
 * {@link ProcessingExecutionTask}.
 * <p>
 * The remote process runs inside a 'screen' session and its output is redirected to a file named after the task
 * resource identifier (see {@link SSHExecutor#asyncOutputFile(String)}). This file is followed over a separate SSH
 * channel, so the screen session itself is not affected.
 * <p>
 * Protocol:
 * <ul>
 *     <li>client &rarr; server: <code>{"taskId": 123}</code> starts streaming the output of the task
 *     (a new request replaces the current one)</li>
 *     <li>server &rarr; client: <code>{"taskId": 123, "type": "output", "line": "..."}</code> for each output line</li>
 *     <li>server &rarr; client: <code>{"taskId": 123, "type": "end", "status": "DONE", "exitCode": 0}</code> when the
 *     task has ended (<code>exitCode</code> is present only if known). The connection is closed afterwards.</li>
 *     <li>server &rarr; client: <code>{"taskId": 123, "type": "error", "message": "..."}</code></li>
 * </ul>
 * Only the owner of the task and the administrators can retrieve its output.
 */
@Component
public class TaskOutputWebSocketHandler extends TextWebSocketHandler implements DisposableBean {
    private static final String SSH_ASYNC_KEY = "tao.ssh.async";
    private static final String SAFE_SESSION_KEY = "task.output.session";
    private static final int SEND_TIME_LIMIT = 10000;
    private static final int SEND_BUFFER_LIMIT = 1024 * 1024;
    private static final int STATUS_CHECK_INTERVAL = 10;

    private final Map<String, OutputFollower> followers = new ConcurrentHashMap<>();
    private final ExecutorService followerPool;
    private final ScheduledExecutorService statusChecker;
    private final Logger logger = Logger.getLogger(TaskOutputWebSocketHandler.class.getName());

    @Autowired
    private ExecutionTaskProvider taskProvider;
    @Autowired
    private NodeDBProvider nodeDBProvider;
    @Autowired
    private UserProvider userProvider;

    public TaskOutputWebSocketHandler() {
        final AtomicInteger counter = new AtomicInteger();
        this.followerPool = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "task-output-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        this.statusChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, "task-output-status");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Not authorized"));
            return;
        }
        super.afterConnectionEstablished(session);
        // Output lines are sent from a worker thread, while errors may be sent from the websocket thread
        session.getAttributes().put(SAFE_SESSION_KEY,
                                    new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_LIMIT,
                                                                            ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final WebSocketSession wsSession = (WebSocketSession) session.getAttributes().get(SAFE_SESSION_KEY);
        if (wsSession == null) {
            return;
        }
        final long taskId;
        try {
            taskId = new JSONObject(message.getPayload()).getLong("taskId");
        } catch (Exception e) {
            send(wsSession, error(null, "Invalid request. Expected {\"taskId\": <id>}"));
            return;
        }
        final OutputFollower follower = new OutputFollower(wsSession, taskId);
        final OutputFollower previous = this.followers.put(session.getId(), follower);
        if (previous != null) {
            previous.stop();
        }
        this.followerPool.submit(follower);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        final OutputFollower follower = this.followers.remove(session.getId());
        if (follower != null) {
            follower.stop();
        }
    }

    @Override
    public void destroy() {
        this.followers.values().forEach(OutputFollower::stop);
        this.followers.clear();
        this.statusChecker.shutdownNow();
        this.followerPool.shutdownNow();
    }

    private boolean canAccess(Principal principal, String ownerId) {
        if (principal.getName().equals(ownerId)) {
            return true;
        }
        final User user = this.userProvider.get(principal.getName());
        return user != null && user.getGroups().stream().anyMatch(g -> Role.ADMIN.equalsIgnoreCase(g.getName()));
    }

    private static boolean isActive(ExecutionStatus status) {
        return status == ExecutionStatus.RUNNING || status == ExecutionStatus.SUSPENDED;
    }

    private static boolean isEnded(ExecutionStatus status) {
        return status == ExecutionStatus.DONE || status == ExecutionStatus.FAILED ||
                status == ExecutionStatus.CANCELLED || status == ExecutionStatus.PENDING_FINALISATION;
    }

    private static JSONObject message(Long taskId, String type) {
        final JSONObject json = new JSONObject();
        if (taskId != null) {
            json.put("taskId", taskId);
        }
        return json.put("type", type);
    }

    private static JSONObject error(Long taskId, String text) {
        return message(taskId, "error").put("message", text);
    }

    private void send(WebSocketSession session, JSONObject json) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json.toString()));
            } catch (IOException e) {
                logger.warning(String.format("Cannot send message to websocket session %s: %s", session.getId(), e.getMessage()));
            }
        }
    }

    /**
     * Follows the output of a single task and forwards it to a websocket session.
     */
    private class OutputFollower implements Runnable {
        private final WebSocketSession session;
        private final long taskId;
        private volatile boolean stopped;
        private volatile SSHExecutor executor;
        private volatile ScheduledFuture<?> statusCheck;

        OutputFollower(WebSocketSession session, long taskId) {
            this.session = session;
            this.taskId = taskId;
        }

        @Override
        public void run() {
            try {
                final ExecutionTask task = taskProvider.get(this.taskId);
                final ExecutionTaskSummary summary = task != null ? taskProvider.getTaskStatus(this.taskId) : null;
                if (!(task instanceof ProcessingExecutionTask) || summary == null) {
                    send(this.session, error(this.taskId, "No processing task found with id " + this.taskId));
                    return;
                }
                if (!canAccess(this.session.getPrincipal(), summary.getUserId())) {
                    send(this.session, error(this.taskId, "Not authorized"));
                    return;
                }
                final ExecutionStatus status = task.getExecutionStatus();
                if (isActive(status)) {
                    follow(task);
                } else if (isEnded(status)) {
                    // The remote output file no longer exists, send what was saved for the task
                    final String log = task.getLog();
                    if (log != null) {
                        log.lines().forEach(this::sendLine);
                    }
                    end(status, null);
                } else {
                    send(this.session, error(this.taskId, "Task has not started yet [status: " + status.friendlyName() + "]"));
                }
            } catch (Exception e) {
                logger.warning(String.format("Cannot stream the output of task %d: %s", this.taskId, e.getMessage()));
                send(this.session, error(this.taskId, e.getMessage()));
            } finally {
                cancelStatusCheck();
                followers.remove(this.session.getId(), this);
            }
        }

        void stop() {
            this.stopped = true;
            cancelStatusCheck();
            final SSHExecutor executor = this.executor;
            if (executor != null) {
                executor.stopFollowing();
            }
        }

        private void follow(ExecutionTask task) throws Exception {
            if (!ConfigurationManager.getInstance().getBooleanValue(SSH_ASYNC_KEY)) {
                throw new IllegalStateException("Live output is available only for asynchronous SSH execution (" + SSH_ASYNC_KEY + "=true)");
            }
            final String host = task.getExecutionNodeHostName();
            final String resourceId = task.getResourceId();
            if (host == null || resourceId == null) {
                throw new IllegalStateException("The execution node or the resource identifier of the task is not set");
            }
            final NodeDescription node = nodeDBProvider.get(host);
            if (node == null) {
                throw new IllegalStateException("Execution node [" + host + "] not found");
            }
            final SSHExecutor executor = new SSHExecutor(host, null, false);
            executor.setUser(node.getUserName());
            executor.setPassword(node.getUserPass());
            if (node.getSshKey() != null) {
                executor.setCertificate(node.getSshKey());
            }
            executor.setOutputConsumer(new OutputConsumer() {
                @Override
                public void consume(String message) {
                    sendLine(message);
                }
            });
            this.executor = executor;
            if (this.stopped) {
                return;
            }
            scheduleStatusCheck();
            final int exitCode = executor.followAsyncOutput(resourceId);
            if (!this.stopped || exitCode != Integer.MIN_VALUE) {
                final ExecutionTaskSummary summary = taskProvider.getTaskStatus(this.taskId);
                end(summary != null ? summary.getTaskStatus() : null, exitCode != Integer.MIN_VALUE ? exitCode : null);
            }
        }

        /**
         * If the remote process is killed (e.g. the task is cancelled), the output file never receives the
         * result marker. Hence, the status of the task is periodically checked and following is stopped
         * when the task is no longer active (one check later, to allow the last lines to be delivered).
         */
        private void scheduleStatusCheck() {
            final AtomicInteger inactiveChecks = new AtomicInteger();
            this.statusCheck = statusChecker.scheduleWithFixedDelay(() -> {
                try {
                    final ExecutionTaskSummary summary = taskProvider.getTaskStatus(this.taskId);
                    if ((summary == null || !isActive(summary.getTaskStatus())) && inactiveChecks.incrementAndGet() > 1) {
                        final SSHExecutor executor = this.executor;
                        if (executor != null) {
                            executor.stopFollowing();
                        }
                    }
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }, STATUS_CHECK_INTERVAL, STATUS_CHECK_INTERVAL, TimeUnit.SECONDS);
        }

        private void cancelStatusCheck() {
            final ScheduledFuture<?> check = this.statusCheck;
            if (check != null) {
                check.cancel(false);
            }
        }

        private void sendLine(String line) {
            if (!this.stopped) {
                send(this.session, message(this.taskId, "output").put("line", line));
            }
        }

        private void end(ExecutionStatus status, Integer exitCode) {
            final JSONObject json = message(this.taskId, "end");
            if (status != null) {
                json.put("status", status.name());
            }
            if (exitCode != null) {
                json.put("exitCode", exitCode);
            }
            send(this.session, json);
            try {
                this.session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }
    }
}
