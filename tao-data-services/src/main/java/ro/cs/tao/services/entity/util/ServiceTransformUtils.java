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
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.component.enums.AuthenticationType;
import ro.cs.tao.component.ogc.WMSComponent;
import ro.cs.tao.component.ogc.WPSComponent;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.docker.Container;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.services.entity.beans.RepositoryBean;
import ro.cs.tao.services.entity.beans.RepositoryTemplateBean;
import ro.cs.tao.services.entity.beans.RepositoryTypeBean;
import ro.cs.tao.services.entity.beans.WebServiceBean;
import ro.cs.tao.services.model.component.*;
import ro.cs.tao.services.model.execution.ExecutionJobInfo;
import ro.cs.tao.services.model.execution.ExecutionTaskInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryTemplate;
import ro.cs.tao.workspaces.RepositoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static List<WMSComponentInfo> toWMSComponentInfos(final List<WMSComponent> components) {
        final List<WMSComponentInfo> results = new ArrayList<>();
        for (WMSComponent component : components) {
            results.add(new WMSComponentInfo(component));
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

    public static List<ProductSetInfo> toProductSetInfos(final List<DataSourceComponent> components, String user) {
        final List<ProductSetInfo> results = new ArrayList<>();
        if (components != null) {
            for (DataSourceComponent component : components) {
                results.add(new ProductSetInfo(component, user));
            }
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

    public static List<WorkflowInfo> toWorkflowInfos(final List<WorkflowDescriptor> descriptors,
                                                     final Map<Long, String> images){
        final List<WorkflowInfo> results = new ArrayList<>();
        for(WorkflowDescriptor workflowDescriptor : descriptors){
            results.add(new WorkflowInfo(workflowDescriptor, images != null ? images.get(workflowDescriptor.getId()) : null));
        }
        return results;
    }

    public static WorkflowInfo toWorkflowInfo(WorkflowDescriptor descriptor, String image){
        return descriptor != null ? new WorkflowInfo(descriptor, image) : null;
    }

    public static RepositoryTypeBean toBean(RepositoryType type) {
        final RepositoryTypeBean bean = new RepositoryTypeBean();
        bean.setId(type.value());
        bean.setName(type.name());
        bean.setDescription(type.friendlyName());
        bean.setUrlPrefix(type.prefix());
        bean.setRootKey(type.rootKey());
        bean.setSingleton(type.singleton());
        bean.setParameters(type.getParameters());
        return bean;
    }

    public static Repository fromBean(RepositoryBean bean) {
        final Repository entity = new Repository();
        entity.setId(bean.getId());
        entity.setName(bean.getName());
        entity.setDescription(bean.getDescription());
        entity.setParameters(bean.getParameters());
        entity.setUrlPrefix(bean.getUrlPrefix());
        entity.setSystem(bean.isSystem());
        entity.setReadOnly(bean.isReadOnly());
        entity.setEditable(bean.isEditable());
        entity.setType(EnumUtils.getEnumConstantByName(RepositoryType.class, bean.getType()));
        entity.setPersistentStorage(bean.isPersistent());
        return entity;
    }

    public static RepositoryBean toBean(Repository entity) {
        final RepositoryBean bean = new RepositoryBean();
        bean.setId(entity.getId());
        bean.setName(entity.getName());
        bean.setDescription(entity.getDescription());
        bean.setParameters(entity.getParameters());
        bean.setUrlPrefix(entity.getUrlPrefix());
        bean.setType(entity.getType().name());
        bean.setRootKey(entity.getType().rootKey());
        bean.setReadOnly(entity.isReadOnly());
        bean.setSystem(entity.isSystem());
        bean.setEditable(entity.isEditable());
        bean.setOrder(entity.getOrder());
        bean.setPersistent(entity.isPersistentStorage());
        bean.setPageItem(entity.getType().pageItem());
        return bean;
    }

    public static RepositoryTemplate fromBean(RepositoryTemplateBean bean) {
        final RepositoryTemplate entity = new RepositoryTemplate();
        entity.setId(bean.getId());
        entity.setName(bean.getName());
        entity.setDescription(bean.getDescription());
        entity.setParameters(bean.getParameters());
        entity.setUrlPrefix(bean.getUrlPrefix());
        entity.setType(EnumUtils.getEnumConstantByName(RepositoryType.class, bean.getType()));
        return entity;
    }

    public static RepositoryTemplateBean toBean(RepositoryTemplate entity) {
        final RepositoryTemplateBean bean = new RepositoryTemplateBean();
        bean.setId(entity.getId());
        bean.setName(entity.getName());
        bean.setDescription(entity.getDescription());
        bean.setParameters(entity.getParameters());
        bean.setUrlPrefix(entity.getUrlPrefix());
        bean.setType(entity.getType().name());
        bean.setRootKey(entity.getType().rootKey());
        return bean;
    }

    public static WebServiceBean toBean(Container container, WebServiceAuthentication authentication) {
        final WebServiceBean bean = new WebServiceBean();
        bean.setId(container.getId());
        bean.setName(container.getName());
        bean.setDescription(container.getDescription());
        bean.setType(container.getType());
        bean.setApplicationPath(container.getApplicationPath());
        bean.setLogo(container.getLogo());
        bean.setApplications(container.getApplications());
        bean.setFormat(container.getFormat());
        bean.setCommonParameters(container.getCommonParameters());
        bean.setFormatNameParameter(container.getFormatNameParameter());
        bean.setTag(container.getTag());
        if (authentication != null) {
            bean.setAuthType(authentication.getType());
            bean.setUser(authentication.getUser());
            //bean.setPassword(Crypto.decrypt(authentication.getPassword(), authentication.getUser()));
            bean.setPassword(authentication.getPassword());
            bean.setLoginUrl(authentication.getLoginUrl());
            bean.setAuthHeader(authentication.getAuthHeader());
        }
        return bean;
    }

    public static Container getContainerPart(WebServiceBean bean) {
        final Container entity = new Container();
        entity.setId(bean.getId());
        entity.setName(bean.getName());
        entity.setDescription(bean.getDescription());
        entity.setType(bean.getType());
        entity.setApplicationPath(bean.getApplicationPath());
        entity.setLogo(bean.getLogo());
        entity.setApplications(bean.getApplications());
        entity.setTag(bean.getTag());
        entity.setFormat(bean.getFormat());
        entity.setCommonParameters(bean.getCommonParameters());
        entity.setFormatNameParameter(bean.getFormatNameParameter());
        return entity;
    }

    public static WebServiceAuthentication getAuthenticationPart(WebServiceBean bean) {
        final WebServiceAuthentication entity = new WebServiceAuthentication();
        entity.setId(bean.getId());
        if (bean.getAuthType() != null) {
            entity.setType(bean.getAuthType());
            entity.setUser(bean.getUser());
            /*if (entity.getUser() != null) {
                entity.setPassword(Crypto.encrypt(bean.getPassword(), bean.getUser()));
            }*/
            entity.setPassword(bean.getPassword());
            entity.setLoginUrl(bean.getLoginUrl());
            entity.setAuthHeader(bean.getAuthHeader());
        } else {
            entity.setType(AuthenticationType.NONE);
        }
        return entity;
    }
}
