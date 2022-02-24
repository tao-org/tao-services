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
package ro.cs.tao.services.entity.util;

import ro.cs.tao.EnumUtils;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.WPSComponent;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.services.entity.beans.WorkspaceBean;
import ro.cs.tao.services.entity.beans.WorkspaceTypeBean;
import ro.cs.tao.services.model.component.DataSourceGroupInfo;
import ro.cs.tao.services.model.component.DataSourceInfo;
import ro.cs.tao.services.model.component.ProcessingComponentInfo;
import ro.cs.tao.services.model.component.WPSComponentInfo;
import ro.cs.tao.services.model.execution.ExecutionJobInfo;
import ro.cs.tao.services.model.execution.ExecutionTaskInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oana H.
 */
public final class ServiceTransformUtils {

    /**
     * Private constructor
     */
    private ServiceTransformUtils() {
        // empty constructor
    }

    public static List<ExecutionJobInfo> toJobInfos(final List<ExecutionJob> executionJobs){
        final List<ExecutionJobInfo> results = new ArrayList<>();
        for(ExecutionJob executionJob : executionJobs){
            results.add(new ExecutionJobInfo(executionJob));
        }
        return results;
    }

    public static List<ExecutionTaskInfo> toTaskInfos(final List<ExecutionTask> executionTasks){
        final List<ExecutionTaskInfo> results = new ArrayList<>();
        for(ExecutionTask executionTask : executionTasks){
            results.add(new ExecutionTaskInfo(executionTask));
        }
        return results;
    }

    public static List<ProcessingComponentInfo> toProcessingComponentInfos(final List<ProcessingComponent> components) {
        final List<ProcessingComponentInfo> results = new ArrayList<>();
        for (ProcessingComponent component : components) {
            results.add(new ProcessingComponentInfo(component));
        }
        return results;
    }

    public static List<WPSComponentInfo> toWPSComponentInfos(final List<WPSComponent> components) {
        final List<WPSComponentInfo> results = new ArrayList<>();
        for (WPSComponent component : components) {
            results.add(new WPSComponentInfo(component));
        }
        return results;
    }

    public static List<DataSourceInfo> toDataSourceInfos(final List<DataSourceComponent> components) {
        final List<DataSourceInfo> results = new ArrayList<>();
        for (DataSourceComponent component : components) {
            results.add(new DataSourceInfo(component));
        }
        return results;
    }

    public static List<DataSourceGroupInfo> toDataSourceGroupInfos(final List<DataSourceComponentGroup> components) {
        final List<DataSourceGroupInfo> results = new ArrayList<>();
        for (DataSourceComponentGroup component : components) {
            results.add(new DataSourceGroupInfo(component));
        }
        return results;
    }

    public static List<WorkflowInfo> toWorkflowInfos(final List<WorkflowDescriptor> descriptors){
        final List<WorkflowInfo> results = new ArrayList<>();
        for(WorkflowDescriptor workflowDescriptor : descriptors){
            results.add(new WorkflowInfo(workflowDescriptor));
        }
        return results;
    }

    public static WorkflowInfo toWorkflowInfo(WorkflowDescriptor descriptor){
        return new WorkflowInfo(descriptor);
    }

    public static WorkspaceTypeBean toBean(WorkspaceType type) {
        final WorkspaceTypeBean bean = new WorkspaceTypeBean();
        bean.setId(type.value());
        bean.setName(type.name());
        bean.setDescription(type.friendlyName());
        bean.setParameters(new ArrayList<>(type.getParameters()));
        bean.getParameters().sort(String::compareTo);
        return bean;
    }

    public static Workspace fromBean(WorkspaceBean bean) {
        final Workspace entity = new Workspace();
        entity.setId(bean.getId());
        entity.setName(bean.getName());
        entity.setDescription(bean.getDescription());
        entity.setParameters(bean.getParameters());
        entity.setUrlPrefix(bean.getUrlPrefix());
        entity.setType(EnumUtils.getEnumConstantByName(WorkspaceType.class, bean.getType()));
        return entity;
    }
}
