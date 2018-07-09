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

package ro.cs.tao.services.entity.impl;

import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.ComponentType;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ContainerInitializer {
    private static PersistenceManager persistenceManager;
    private static ContainerService containerService;
    private static ComponentService componentService;
    private static WorkflowService workflowService;

    public static void setPersistenceManager(PersistenceManager persistenceManager) {
        ContainerInitializer.persistenceManager = persistenceManager;
    }

    public static void setContainerService(ContainerService containerService) {
        ContainerInitializer.containerService = containerService;
    }

    public static void setComponentService(ComponentService componentService) {
        ContainerInitializer.componentService = componentService;
    }

    public static void setWorkflowService(WorkflowService workflowService) {
        ContainerInitializer.workflowService = workflowService;
    }

    public static void initSnap(String snapContainerName, String snapPath) {
        Container snapContainer = null;
        try {
            snapContainer = persistenceManager.getContainerById(snapContainerName);
        } catch (PersistenceException ignored) {
        }
        if (snapContainer == null) {
            snapContainer = containerService.initSNAP(snapContainerName, snapPath);
            Logger.getLogger(ContainerInitializer.class.getName()).info("Registered SNAP container");
        }
    }

    public static void initOtb(String otbContainerName, String otbPath) {
        Container otbContainer = null;
        try {
            otbContainer = persistenceManager.getContainerById(otbContainerName);
        } catch (PersistenceException ignored) {
        }
        if (otbContainer == null) {
            otbContainer = containerService.initOTB(otbContainerName, otbPath);
            Logger.getLogger(ContainerInitializer.class.getName()).info("Registered OTB container");
        }
    }

    public static DataSourceComponent initDataSourceComponent(String sensor, String dataSource) throws PersistenceException {
        // let's have a DataSourceComponent
        String componentId = sensor + "-" + dataSource;
        DataSourceComponent dataSourceComponent;
        dataSourceComponent = persistenceManager.getDataSourceInstance(componentId);
        if (dataSourceComponent == null) {
            dataSourceComponent = new DataSourceComponent(sensor, dataSource);
            dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
            dataSourceComponent.setLabel(dataSourceComponent.getSensorName() + " from " + dataSourceComponent.getDataSourceName());
            dataSourceComponent.setVersion("1.0");
            dataSourceComponent.setDescription(dataSourceComponent.getId());
            dataSourceComponent.setAuthors("TAO Team");
            dataSourceComponent.setCopyright("(C) TAO Team");
            dataSourceComponent.setNodeAffinity("Any");
            persistenceManager.saveDataSourceComponent(dataSourceComponent);
        }
        return dataSourceComponent;
    }

    public static WorkflowDescriptor initWorkflow1() throws PersistenceException {
        // Initialize test workflows
        // Workflow 1: SNAP NDVI -> OTB RESAMPLE
        WorkflowDescriptor descriptor1 = new WorkflowDescriptor();
        descriptor1.setName("SNAP NDVI + OTB RESAMPLE workflow");
        descriptor1.setStatus(Status.DRAFT);
        descriptor1.setCreated(LocalDateTime.now());
        descriptor1.setActive(true);
        descriptor1.setUserName("admin");
        descriptor1.setVisibility(Visibility.PRIVATE);
        descriptor1 = persistenceManager.saveWorkflowDescriptor(descriptor1);
        addNodes1(descriptor1);
        return descriptor1;
    }

    public static WorkflowDescriptor initWorkflow2() throws PersistenceException {
        // Workflow 1: OTB RI -> OTB RESAMPLE
        WorkflowDescriptor descriptor2 = new WorkflowDescriptor();
        descriptor2.setName("OTB Radiometric Indices + OTB RESAMPLE workflow");
        descriptor2.setStatus(Status.DRAFT);
        descriptor2.setCreated(LocalDateTime.now());
        descriptor2.setActive(true);
        descriptor2.setUserName("admin");
        descriptor2.setVisibility(Visibility.PRIVATE);
        persistenceManager.saveWorkflowDescriptor(descriptor2);
        addNodes2(descriptor2);
        return descriptor2;
    }

    public static WorkflowDescriptor initWorkflow3() throws PersistenceException {
        // Workflow 3: OTB RESAMPLE -> {OTB RI NDVI + OTB RI TNDVI} -> OTB CONCATENATE
        WorkflowDescriptor descriptor3 = new WorkflowDescriptor();
        descriptor3.setName("OTB Resample, NDVI, TNDVI and Concatenate");
        descriptor3.setStatus(Status.DRAFT);
        descriptor3.setCreated(LocalDateTime.now());
        descriptor3.setActive(true);
        descriptor3.setUserName("admin");
        descriptor3.setVisibility(Visibility.PRIVATE);
        persistenceManager.saveWorkflowDescriptor(descriptor3);
        addNodes3(descriptor3);
        return descriptor3;
    }

    public static WorkflowDescriptor initWorkflow4() throws PersistenceException {
        // Workflow 4: SNAP Resample -> {SNAP NDVI + SNAP MSAVI} -> OTB CONCATENATE
        WorkflowDescriptor descriptor4 = new WorkflowDescriptor();
        descriptor4.setName("SNAP Resample, NDVI, MSAVI and OTB Concatenate");
        descriptor4.setStatus(Status.DRAFT);
        descriptor4.setCreated(LocalDateTime.now());
        descriptor4.setActive(true);
        descriptor4.setUserName("admin");
        descriptor4.setVisibility(Visibility.PRIVATE);
        persistenceManager.saveWorkflowDescriptor(descriptor4);
        addNodes4(descriptor4);
        return descriptor4;
    }

    public static WorkflowDescriptor initWorkflow5(DataSourceComponent dataSourceComponent) throws PersistenceException {
        // Workflow 5: Data query + fetch 2 products -> Group { SNAP NDVI + OTB RESAMPLE }
        WorkflowDescriptor descriptor5 = new WorkflowDescriptor();
        descriptor5.setName("AWS Download, { SNAP NDVI and OTB Resample }");
        descriptor5.setStatus(Status.DRAFT);
        descriptor5.setCreated(LocalDateTime.now());
        descriptor5.setActive(true);
        descriptor5.setUserName("admin");
        descriptor5.setVisibility(Visibility.PRIVATE);
        persistenceManager.saveWorkflowDescriptor(descriptor5);
        addNodes5(descriptor5, dataSourceComponent);
        return descriptor5;
    }

    public static WorkflowDescriptor initWorkflow6(DataSourceComponent dataSourceComponent) throws PersistenceException {
        // Workflow 6: Data query + fetch 2 products -> SNAP NDVI + OTB RESAMPLE ( without grouping)
        WorkflowDescriptor descriptor6 = new WorkflowDescriptor();
        descriptor6.setName("AWS Download, SNAP NDVI and OTB Resample (no group)");
        descriptor6.setStatus(Status.DRAFT);
        descriptor6.setCreated(LocalDateTime.now());
        descriptor6.setActive(true);
        descriptor6.setUserName("admin");
        descriptor6.setVisibility(Visibility.PRIVATE);
        persistenceManager.saveWorkflowDescriptor(descriptor6);
        addNodes6(descriptor6, dataSourceComponent);
        return descriptor6;

    }

    private static void addNodes1(WorkflowDescriptor parent) throws PersistenceException {
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP NDVI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("snap-ndviop");
        node1.setComponentType(ComponentType.PROCESSING);
        node1.setCreated(LocalDateTime.now());
        node1 = workflowService.addNode(parent.getId(), node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("RigidTransformResample");
        node2.setComponentType(ComponentType.PROCESSING);
        node2.addCustomValue("transform_type_id_scalex_number", "0.5");
        node2.addCustomValue("transform_type_id_scaley_number", "0.5");
        node2.setCreated(LocalDateTime.now());
        node2 = workflowService.addNode(parent.getId(), node2);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node2.getId(), component2.getSources().get(0).getId());
    }

    private static void addNodes2(WorkflowDescriptor parent) throws PersistenceException {
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("OTB RI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("RadiometricIndices");
        node1.setComponentType(ComponentType.PROCESSING);
        node1.addCustomValue("list_str", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        node1 = workflowService.addNode(parent.getId(), node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("RigidTransformResample");
        node2.setComponentType(ComponentType.PROCESSING);
        node2.addCustomValue("transform_type_id_scalex_number", "0.5");
        node2.addCustomValue("transform_type_id_scaley_number", "0.5");
        node2.setCreated(LocalDateTime.now());
        node2 = workflowService.addNode(parent.getId(), node2);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node2.getId(), component2.getSources().get(0).getId());
    }

    private static void addNodes3(WorkflowDescriptor parent) throws PersistenceException {
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("OTB Resample");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("RigidTransformResample");
        node1.setComponentType(ComponentType.PROCESSING);
        node1.addCustomValue("transform_type_id_scalex_number", "0.5");
        node1.addCustomValue("transform_type_id_scaley_number", "0.5");
        node1.setCreated(LocalDateTime.now());
        node1 = workflowService.addNode(parent.getId(), node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Indices");
        node2.setxCoord(600);
        node2.setyCoord(150);
        node2.setComponentId("RadiometricIndices");
        node2.setComponentType(ComponentType.PROCESSING);
        node2.addCustomValue("list_str", "Vegetation:NDVI");
        node2.setCreated(LocalDateTime.now());
        node2 = workflowService.addNode(parent.getId(), node2);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("OTB Indices");
        node3.setxCoord(600);
        node3.setyCoord(750);
        node3.setComponentId("RadiometricIndices");
        node3.setComponentType(ComponentType.PROCESSING);
        node3.addCustomValue("list_str", "Vegetation:TNDVI");
        node3.setCreated(LocalDateTime.now());
        node3 = workflowService.addNode(parent.getId(), node3);

        WorkflowNodeDescriptor node4 = new WorkflowNodeDescriptor();
        node4.setWorkflow(parent);
        node4.setName("OTB Concatenate");
        node4.setxCoord(900);
        node4.setyCoord(600);
        node4.setComponentId("ConcatenateImages");
        node4.setComponentType(ComponentType.PROCESSING);
        node4.setCreated(LocalDateTime.now());
        node4 = workflowService.addNode(parent.getId(), node4);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ProcessingComponent component3 = componentService.findById(node3.getComponentId());
        ProcessingComponent component4 = componentService.findById(node4.getComponentId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node2.getId(), component2.getSources().get(0).getId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node3.getId(), component3.getSources().get(0).getId());

        workflowService.addLink(node2.getId(), component2.getTargets().get(0).getId(),
                                node4.getId(), component4.getSources().get(0).getId());
        workflowService.addLink(node3.getId(), component3.getTargets().get(0).getId(),
                                node4.getId(), component4.getSources().get(0).getId());
    }

    private static void addNodes4(WorkflowDescriptor parent) throws PersistenceException {
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP Resample");
        node1.setxCoord(300);
        node1.setyCoord(150);
        node1.setComponentId("snap-resample");
        node1.setComponentType(ComponentType.PROCESSING);
        node1.addCustomValue("targetResolution", "60");
        node1.setCreated(LocalDateTime.now());
        node1 = workflowService.addNode(parent.getId(), node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("SNAP NDVI");
        node2.setxCoord(500);
        node2.setyCoord(50);
        node2.setComponentId("snap-ndviop");
        node2.setComponentType(ComponentType.PROCESSING);
        node2.setCreated(LocalDateTime.now());
        node2 = workflowService.addNode(parent.getId(), node2);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("SNAP MSAVI");
        node3.setxCoord(500);
        node3.setyCoord(250);
        node3.setComponentId("snap-msaviop");
        node3.setComponentType(ComponentType.PROCESSING);
        node3.setCreated(LocalDateTime.now());
        node3 = workflowService.addNode(parent.getId(), node3);

        WorkflowNodeDescriptor node4 = new WorkflowNodeDescriptor();
        node4.setWorkflow(parent);
        node4.setName("OTB Combine");
        node4.setxCoord(800);
        node4.setyCoord(150);
        node4.setComponentId("ConcatenateImages");
        node4.setComponentType(ComponentType.PROCESSING);
        node4.setCreated(LocalDateTime.now());
        node4 = workflowService.addNode(parent.getId(), node4);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ProcessingComponent component3 = componentService.findById(node3.getComponentId());
        ProcessingComponent component4 = componentService.findById(node4.getComponentId());


        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node2.getId(), component2.getSources().get(0).getId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node3.getId(), component3.getSources().get(0).getId());

        workflowService.addLink(node2.getId(), component2.getTargets().get(0).getId(),
                                node4.getId(), component4.getSources().get(0).getId());
        workflowService.addLink(node3.getId(), component3.getTargets().get(0).getId(),
                                node4.getId(), component4.getSources().get(0).getId());
    }

    private static void addNodes5(WorkflowDescriptor parent, DataSourceComponent component) throws PersistenceException {
        WorkflowNodeDescriptor dsNode = new WorkflowNodeDescriptor();
        dsNode.setWorkflow(parent);
        dsNode.setName("AWS Download");
        dsNode.setxCoord(300);
        dsNode.setyCoord(500);
        dsNode.setComponentId(component.getId());
        dsNode.setComponentType(ComponentType.DATASOURCE);
        dsNode.setCreated(LocalDateTime.now());
        dsNode = workflowService.addNode(parent.getId(), dsNode);

        WorkflowNodeGroupDescriptor grpNode = new WorkflowNodeGroupDescriptor();
        grpNode.setWorkflow(parent);
        grpNode.setName("Group-1");
        grpNode.setxCoord(100);
        grpNode.setyCoord(100);
        grpNode.setCreated(LocalDateTime.now());
        grpNode.setPreserveOutput(true);

        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP NDVI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("snap-ndviop");
        node1.setComponentType(ComponentType.PROCESSING);
        node1.setCreated(LocalDateTime.now());
        node1.setPreserveOutput(true);
        node1 = workflowService.addNode(parent.getId(), node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("RigidTransformResample");
        node2.setComponentType(ComponentType.PROCESSING);
        node2.addCustomValue("transform_type_id_scalex_number", "0.5");
        node2.addCustomValue("transform_type_id_scaley_number", "0.5");
        node2.setCreated(LocalDateTime.now());
        node2.setPreserveOutput(true);
        node2 = workflowService.addNode(parent.getId(), node2);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());

        workflowService.addLink(node1.getId(), component1.getTargets().get(0).getId(),
                                node2.getId(), component2.getSources().get(0).getId());

        workflowService.addGroup(parent.getId(), grpNode, dsNode.getId(),
                                 new WorkflowNodeDescriptor[] { node1, node2 });

        Query dsQuery = new Query();
        dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor("Sentinel2");
        dsQuery.setDataSource("Amazon Web Services");
        dsQuery.setPageNumber(1);
        dsQuery.setPageSize(10);
        dsQuery.setLimit(2);
        Map<String, String> values = new HashMap<>();
        values.put("beginPosition", "2018-01-01");
        values.put("endPosition", "2018-01-01");
        values.put("footprint", "POLYGON((22.8042573604346 43.8379609098684, " +
                "24.83885442747927 43.8379609098684, 24.83885442747927 44.795645304033826, " +
                "22.8042573604346 44.795645304033826, 22.8042573604346 43.8379609098684))");
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(dsNode.getId());
        persistenceManager.saveQuery(dsQuery);

    }

    private static void addNodes6(WorkflowDescriptor parent, DataSourceComponent component) throws PersistenceException {
        WorkflowNodeDescriptor dsNode = new WorkflowNodeDescriptor();
        dsNode.setWorkflow(parent);
        dsNode.setName("AWS Download");
        dsNode.setxCoord(300);
        dsNode.setyCoord(500);
        dsNode.setComponentId(component.getId());
        dsNode.setComponentType(ComponentType.DATASOURCE);
        dsNode.setCreated(LocalDateTime.now());
        dsNode = workflowService.addNode(parent.getId(), dsNode);

        WorkflowNodeDescriptor node11 = new WorkflowNodeDescriptor();
        node11.setWorkflow(parent);
        node11.setName("SNAP NDVI");
        node11.setxCoord(400);
        node11.setyCoord(400);
        node11.setComponentId("snap-ndviop");
        node11.setComponentType(ComponentType.PROCESSING);
        node11.setCreated(LocalDateTime.now());
        node11.setPreserveOutput(true);
        node11 = workflowService.addNode(parent.getId(), node11);

        WorkflowNodeDescriptor node12 = new WorkflowNodeDescriptor();
        node12.setWorkflow(parent);
        node12.setName("SNAP RVI");
        node12.setxCoord(400);
        node12.setyCoord(500);
        node12.setComponentId("snap-rviop");
        node12.setComponentType(ComponentType.PROCESSING);
        node12.setCreated(LocalDateTime.now());
        node12.setPreserveOutput(true);
        node12 = workflowService.addNode(parent.getId(), node12);

        WorkflowNodeDescriptor node13 = new WorkflowNodeDescriptor();
        node13.setWorkflow(parent);
        node13.setName("SNAP SAVI");
        node13.setxCoord(400);
        node13.setyCoord(600);
        node13.setComponentId("snap-saviop");
        node13.setComponentType(ComponentType.PROCESSING);
        node13.setCreated(LocalDateTime.now());
        node13.setPreserveOutput(true);
        node13 = workflowService.addNode(parent.getId(), node13);

        WorkflowNodeDescriptor node21 = new WorkflowNodeDescriptor();
        node21.setWorkflow(parent);
        node21.setName("OTB Resample");
        node21.setxCoord(500);
        node21.setyCoord(400);
        node21.setComponentId("RigidTransformResample");
        node21.setComponentType(ComponentType.PROCESSING);
        node21.addCustomValue("transform_type_id_scalex_number", "0.5");
        node21.addCustomValue("transform_type_id_scaley_number", "0.5");
        node21.setCreated(LocalDateTime.now());
        node21.setPreserveOutput(true);
        node21 = workflowService.addNode(parent.getId(), node21);

        WorkflowNodeDescriptor node22 = new WorkflowNodeDescriptor();
        node22.setWorkflow(parent);
        node22.setName("OTB Resample");
        node22.setxCoord(500);
        node22.setyCoord(500);
        node22.setComponentId("RigidTransformResample");
        node22.setComponentType(ComponentType.PROCESSING);
        node22.addCustomValue("transform_type_id_scalex_number", "0.5");
        node22.addCustomValue("transform_type_id_scaley_number", "0.5");
        node22.setCreated(LocalDateTime.now());
        node22.setPreserveOutput(true);
        node22 = workflowService.addNode(parent.getId(), node22);

        WorkflowNodeDescriptor node23 = new WorkflowNodeDescriptor();
        node23.setWorkflow(parent);
        node23.setName("OTB Resample");
        node23.setxCoord(500);
        node23.setyCoord(600);
        node23.setComponentId("RigidTransformResample");
        node23.setComponentType(ComponentType.PROCESSING);
        node23.addCustomValue("transform_type_id_scalex_number", "0.5");
        node23.addCustomValue("transform_type_id_scaley_number", "0.5");
        node23.setCreated(LocalDateTime.now());
        node23.setPreserveOutput(true);
        node23 = workflowService.addNode(parent.getId(), node23);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("OTB Concatenate");
        node3.setxCoord(600);
        node3.setyCoord(500);
        node3.setComponentId("ConcatenateImages");
        node3.setComponentType(ComponentType.PROCESSING);
        node3.setCreated(LocalDateTime.now());
        node3.setPreserveOutput(true);
        node3 = workflowService.addNode(parent.getId(), node3);

        ProcessingComponent component11 = componentService.findById(node11.getComponentId());
        ProcessingComponent component12 = componentService.findById(node12.getComponentId());
        ProcessingComponent component13 = componentService.findById(node13.getComponentId());
        ProcessingComponent component21 = componentService.findById(node21.getComponentId());
        ProcessingComponent component22 = componentService.findById(node22.getComponentId());
        ProcessingComponent component23 = componentService.findById(node23.getComponentId());
        ProcessingComponent component3 = componentService.findById(node3.getComponentId());

        workflowService.addLink(dsNode.getId(), component.getTargets().get(0).getId(),
                                node11.getId(), component11.getSources().get(0).getId());
        workflowService.addLink(dsNode.getId(), component.getTargets().get(0).getId(),
                                node12.getId(), component12.getSources().get(0).getId());
        workflowService.addLink(dsNode.getId(), component.getTargets().get(0).getId(),
                                node13.getId(), component13.getSources().get(0).getId());

        workflowService.addLink(node11.getId(), component11.getTargets().get(0).getId(),
                                node21.getId(), component21.getSources().get(0).getId());
        workflowService.addLink(node12.getId(), component12.getTargets().get(0).getId(),
                                node22.getId(), component22.getSources().get(0).getId());
        workflowService.addLink(node13.getId(), component13.getTargets().get(0).getId(),
                                node23.getId(), component23.getSources().get(0).getId());

        workflowService.addLink(node21.getId(), component21.getTargets().get(0).getId(),
                                node3.getId(), component3.getSources().get(0).getId());
        workflowService.addLink(node22.getId(), component22.getTargets().get(0).getId(),
                                node3.getId(), component3.getSources().get(0).getId());
        workflowService.addLink(node23.getId(), component23.getTargets().get(0).getId(),
                                node3.getId(), component3.getSources().get(0).getId());

        Query dsQuery = new Query();
        dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor("Sentinel2");
        dsQuery.setDataSource("Amazon Web Services");
        dsQuery.setPageNumber(1);
        dsQuery.setPageSize(10);
        dsQuery.setLimit(2);
        Map<String, String> values = new HashMap<>();
        values.put("beginPosition", "2018-01-01");
        values.put("endPosition", "2018-01-01");
        values.put("footprint", "POLYGON((22.8042573604346 43.8379609098684, " +
                "24.83885442747927 43.8379609098684, 24.83885442747927 44.795645304033826, " +
                "22.8042573604346 44.795645304033826, 22.8042573604346 43.8379609098684))");
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(dsNode.getId());
        /*dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor("Sentinel2");
        dsQuery.setDataSource("Local Database");
        dsQuery.setPageNumber(1);
        dsQuery.setPageSize(10);
        dsQuery.setLimit(2);
        Map<String, String> values = new HashMap<>();
        values.put("acquisition_date", "2018-01-01");
        values.put("geometry", "POLYGON((22.8042573604346 43.8379609098684, " +
                "24.83885442747927 43.8379609098684, 24.83885442747927 44.795645304033826, " +
                "22.8042573604346 44.795645304033826, 22.8042573604346 43.8379609098684))");
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(dsNode.getId());*/
        persistenceManager.saveQuery(dsQuery);

    }
}
