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
import ro.cs.tao.component.ogc.OGCProcessComponent;
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
 * Utility class for transforming various service-related objects into specific
 * corresponding DTO (Data Transfer Object) representations. This class provides
 * methods for mapping and converting between entity models and their respective
 * DTOs to facilitate interoperability within the system.
 *
 * This class is not meant to be instantiated as it provides only static utility
 * methods.
 *
 * @author Oana H.
 */
public final class ServiceTransformUtils {

    /**
     * Private constructor
     */
    private ServiceTransformUtils() {
        // empty constructor
    }

    /**
     * Converts a list of ExecutionJob objects into a list of ExecutionJobInfo objects.
     *
     * @param executionJobs the list of ExecutionJob objects to be converted
     * @return a list of ExecutionJobInfo objects created from the provided ExecutionJob objects
     */
    public static List<ExecutionJobInfo> toJobInfos(final List<ExecutionJob> executionJobs){
        final List<ExecutionJobInfo> results = new ArrayList<>();
        for(ExecutionJob executionJob : executionJobs){
            results.add(new ExecutionJobInfo(executionJob));
        }
        return results;
    }

    /**
     * Converts a list of ExecutionTask objects into a list of ExecutionTaskInfo objects.
     *
     * @param executionTasks the list of ExecutionTask objects to be converted
     * @return a list of ExecutionTaskInfo objects created from the provided ExecutionTask objects
     */
    public static List<ExecutionTaskInfo> toTaskInfos(final List<ExecutionTask> executionTasks){
        final List<ExecutionTaskInfo> results = new ArrayList<>();
        for(ExecutionTask executionTask : executionTasks){
            results.add(new ExecutionTaskInfo(executionTask));
        }
        return results;
    }

    /**
     * Converts a list of ProcessingComponent objects into a list of ProcessingComponentInfo objects.
     *
     * @param components the list of ProcessingComponent objects to be converted
     * @return a list of ProcessingComponentInfo objects created from the provided ProcessingComponent objects
     */
    public static List<ProcessingComponentInfo> toProcessingComponentInfos(final List<ProcessingComponent> components) {
        final List<ProcessingComponentInfo> results = new ArrayList<>();
        for (ProcessingComponent component : components) {
            results.add(new ProcessingComponentInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code WPSComponent} objects into a list of {@code WPSComponentInfo} objects.
     * Each {@code WPSComponentInfo} is created by mapping data from a corresponding {@code WPSComponent}.
     *
     * @param components the list of {@code WPSComponent} objects to be converted
     * @return a list of {@code WPSComponentInfo} objects created from the provided {@code WPSComponent} objects
     */
    public static List<WPSComponentInfo> toWPSComponentInfos(final List<WPSComponent> components) {
        final List<WPSComponentInfo> results = new ArrayList<>();
        for (WPSComponent component : components) {
            results.add(new WPSComponentInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code WMSComponent} objects into a list of {@code WMSComponentInfo} objects.
     * Each {@code WMSComponentInfo} is created by mapping data from a corresponding {@code WMSComponent}.
     *
     * @param components the list of {@code WMSComponent} objects to be converted
     * @return a list of {@code WMSComponentInfo} objects created from the provided {@code WMSComponent} objects
     */
    public static List<WMSComponentInfo> toWMSComponentInfos(final List<WMSComponent> components) {
        final List<WMSComponentInfo> results = new ArrayList<>();
        for (WMSComponent component : components) {
            results.add(new WMSComponentInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code OGCProcessComponent} objects into a list of {@code OGCProcessComponentInfo} objects.
     * Each {@code OGCProcessComponentInfo} is created by mapping data from a corresponding {@code OGCProcessComponent}.
     *
     * @param components the list of {@code OGCProcessComponent} objects to be converted
     * @return a list of {@code OGCProcessComponentInfo} objects created from the provided {@code OGCProcessComponent} objects
     */
    public static List<OGCProcessComponentInfo> toOGCProcessComponentInfos(final List<OGCProcessComponent> components) {
        final List<OGCProcessComponentInfo> results = new ArrayList<>();
        for (OGCProcessComponent component : components) {
            results.add(new OGCProcessComponentInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code DataSourceComponent} objects into a list of {@code DataSourceInfo} objects.
     * Each {@code DataSourceInfo} is created by mapping data from a corresponding {@code DataSourceComponent}.
     *
     * @param components the list of {@code DataSourceComponent} objects to be converted
     * @return a list of {@code DataSourceInfo} objects created from the provided {@code DataSourceComponent} objects
     */
    public static List<DataSourceInfo> toDataSourceInfos(final List<DataSourceComponent> components) {
        final List<DataSourceInfo> results = new ArrayList<>();
        for (DataSourceComponent component : components) {
            results.add(new DataSourceInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code DataSourceComponent} objects into a list of {@code ProductSetInfo} objects.
     * Each {@code ProductSetInfo} is created by mapping data from the corresponding {@code DataSourceComponent}
     * and associating it with the provided user.
     *
     * @param components the list of {@code DataSourceComponent} objects to be converted; may be {@code null}
     * @param user the user information to be associated with each {@code ProductSetInfo}
     * @return a list of {@code ProductSetInfo} objects created from the provided {@code DataSourceComponent} objects;
     *         returns an empty list if {@code components} is {@code null}
     */
    public static List<ProductSetInfo> toProductSetInfos(final List<DataSourceComponent> components, String user) {
        final List<ProductSetInfo> results = new ArrayList<>();
        if (components != null) {
            for (DataSourceComponent component : components) {
                results.add(new ProductSetInfo(component, user));
            }
        }
        return results;
    }

    /**
     * Converts a list of {@code DataSourceComponentGroup} objects into a list of {@code DataSourceGroupInfo} objects.
     * Each {@code DataSourceGroupInfo} instance is created by mapping data from a corresponding {@code DataSourceComponentGroup}.
     *
     * @param components the list of {@code DataSourceComponentGroup} objects to be converted
     * @return a list of {@code DataSourceGroupInfo} objects created from the provided {@code DataSourceComponentGroup} objects
     */
    public static List<DataSourceGroupInfo> toDataSourceGroupInfos(final List<DataSourceComponentGroup> components) {
        final List<DataSourceGroupInfo> results = new ArrayList<>();
        for (DataSourceComponentGroup component : components) {
            results.add(new DataSourceGroupInfo(component));
        }
        return results;
    }

    /**
     * Converts a list of {@code WorkflowDescriptor} objects into a list of {@code WorkflowInfo} objects.
     * Each {@code WorkflowInfo} instance is created by mapping data from a corresponding {@code WorkflowDescriptor}
     * and optionally associating it with an image from the provided map.
     *
     * @param descriptors the list of {@code WorkflowDescriptor} objects to be converted
     * @param images a map associating workflow IDs to image URLs; can be {@code null} if no image mapping is needed
     * @return a list of {@code WorkflowInfo} objects created from the provided {@code WorkflowDescriptor} objects
     */
    public static List<WorkflowInfo> toWorkflowInfos(final List<WorkflowDescriptor> descriptors,
                                                     final Map<Long, String> images){
        final List<WorkflowInfo> results = new ArrayList<>();
        for (WorkflowDescriptor workflowDescriptor : descriptors){
            results.add(new WorkflowInfo(workflowDescriptor, images != null ? images.get(workflowDescriptor.getId()) : null));
        }
        return results;
    }

    public static WorkflowInfo toWorkflowInfo(WorkflowDescriptor descriptor, String image){
        return descriptor != null ? new WorkflowInfo(descriptor, image) : null;
    }

    /**
     * Converts a given RepositoryType instance into a RepositoryTypeBean instance.
     *
     * @param type the RepositoryType instance to be converted
     * @return the resulting RepositoryTypeBean instance containing data from the provided RepositoryType
     */
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

    /**
     * Converts a given RepositoryBean instance into a Repository instance.
     *
     * @param bean the RepositoryBean instance to be converted
     * @return the resulting Repository instance containing data mapped from the provided RepositoryBean
     */
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

    /**
     * Converts a given Repository entity into a RepositoryBean instance.
     * Maps the properties of the Repository entity to their corresponding fields in the RepositoryBean.
     *
     * @param entity the Repository instance to be converted
     * @return a RepositoryBean instance populated with data mapped from the provided Repository entity
     */
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

    /**
     * Converts a given RepositoryTemplateBean instance into a RepositoryTemplate instance.
     * Maps the properties of the RepositoryTemplateBean to their corresponding fields
     * in the RepositoryTemplate.
     *
     * @param bean the RepositoryTemplateBean instance to be converted
     * @return a RepositoryTemplate instance populated with data from the provided RepositoryTemplateBean
     */
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

    /**
     * Converts a given RepositoryTemplate instance into a RepositoryTemplateBean instance.
     * Maps the properties of the RepositoryTemplate to their corresponding fields
     * in the RepositoryTemplateBean.
     *
     * @param entity the RepositoryTemplate instance to be converted
     * @return a RepositoryTemplateBean instance containing data from the provided RepositoryTemplate
     */
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

    /**
     * Converts a given {@link Container} instance and an optional {@link WebServiceAuthentication} instance
     * into a {@link WebServiceBean} instance. Populates the properties of the WebServiceBean based on the
     * provided input objects.
     *
     * @param container the {@link Container} object from which to populate the {@link WebServiceBean} properties
     * @param authentication the optional {@link WebServiceAuthentication} object to map authentication details into the bean
     * @return a {@link WebServiceBean} instance populated with data from the provided container and authentication
     */
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

    /**
     * Creates and returns a {@link Container} instance by mapping properties from the given {@link WebServiceBean}.
     *
     * @param bean the {@link WebServiceBean} source from which to populate the {@link Container} object
     * @return a {@link Container} object populated with data from the provided {@link WebServiceBean}
     */
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

    /**
     * Converts a given {@link WebServiceBean} instance into a {@link WebServiceAuthentication} instance.
     * Populates the authentication details in the resulting object based on the properties of the input bean.
     *
     * @param bean the {@link WebServiceBean} containing authentication information to be converted
     * @return a {@link WebServiceAuthentication} instance populated with data from the provided bean
     */
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
