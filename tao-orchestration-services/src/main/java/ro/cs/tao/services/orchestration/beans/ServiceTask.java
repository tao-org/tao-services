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

package ro.cs.tao.services.orchestration.beans;

import java.time.LocalDateTime;

/**
 * The ServiceTask class represents a task executed within a service-oriented workflow.
 * It contains information about the task's identifier, associated workflow node,
 * resource allocation, execution host, and start time.
 *
 * This class provides constructors to initialize its attributes and getter/setter
 * methods to access and modify them.
 */
public class ServiceTask {
    private Long id;
    private Long workflowNodeId;
    private String resourceId;
    private String executionNodeHostName;
    private LocalDateTime startTime;

    public ServiceTask() {}

    public ServiceTask(Long id, Long workflowNodeId, String resourceId, String executionNodeHostName, LocalDateTime startTime) {
        this.id = id;
        this.workflowNodeId = workflowNodeId;
        this.resourceId = resourceId;
        this.executionNodeHostName = executionNodeHostName;
        this.startTime = startTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkflowNodeId() { return workflowNodeId; }
    public void setWorkflowNodeId(Long workflowNodeId) { this.workflowNodeId = workflowNodeId; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getExecutionNodeHostName() { return executionNodeHostName; }
    public void setExecutionNodeHostName(String executionNodeHostName) { this.executionNodeHostName = executionNodeHostName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
}
