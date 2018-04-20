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
package ro.cs.tao.services.entity.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.component.GroupComponent;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.entity.demo.OTBDemo;
import ro.cs.tao.services.entity.demo.SNAPDemo;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.utils.Platform;
import ro.cs.tao.workflow.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/workflow")
public class WorkflowController extends DataEntityController<WorkflowDescriptor, WorkflowService> {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private GroupComponentService groupComponentService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private PersistenceManager persistenceManager;

    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> initialize() throws PersistenceException {

        Platform currentPlatform = Platform.getCurrentPlatform();
        // Initialize OTB test otbContainer
        Container otbContainer = null;
        try {
            otbContainer = persistenceManager.getContainerById("690d6747-7b76-4290-bb5d-87d775b5d23a");
        } catch (PersistenceException ignored) { }
        if (otbContainer == null) {
            String otbPath = currentPlatform.getId().equals(Platform.ID.win) ?
                    "C:\\Tools\\OTB-6.4.0\\bin" : "/opt/OTB-6.4.0-Linux64/bin";
            otbContainer = containerService.initOTB(otbPath);
        }

        // Initialize SNAP test otbContainer
        Container snapContainer = null;
        try {
            snapContainer = persistenceManager.getContainerById("caefe468-3830-45ba-a7b8-053397460899");
        } catch (PersistenceException ignored) { }
        if (snapContainer == null) {
            String snapPath = currentPlatform.getId().equals(Platform.ID.win) ?
                    "" : "/opt/snap/bin";
            snapContainer = containerService.initSNAP(snapPath);
        }
        List<Application> applications = snapContainer.getApplications();
        if (applications == null || applications.size() == 0) {
            Application application = new Application();
            application.setName("snap-ndvi");
            application.setPath(snapContainer.getApplicationPath());
            snapContainer.addApplication(application);
            application = new Application();
            application.setName("snap-s2rep");
            application.setPath(snapContainer.getApplicationPath());
            snapContainer.addApplication(application);
            application = new Application();
            application.setName("snap-msavi");
            application.setPath(snapContainer.getApplicationPath());
            snapContainer.addApplication(application);
            application = new Application();
            application.setName("snap-resample");
            application.setPath(snapContainer.getApplicationPath());
            snapContainer.addApplication(application);
            persistenceManager.updateContainer(snapContainer);
        }

        // Initialize OTB processing components
        ProcessingComponent component;
        String componentId = "otbcli_RigidTransformResample";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = OTBDemo.rigidTransform(otbContainer);
            componentService.save(component);
        }
        componentId = "otbcli_RadiometricIndices";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = OTBDemo.radiometricIndices(otbContainer);
            componentService.save(component);
        }
        componentId = "otbcli_ConcatenateImages";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = OTBDemo.concatenateImages(otbContainer);
            componentService.save(component);
        }

        // Initialize SNAP processing components
        componentId = "snap-s2rep";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = SNAPDemo.s2rep();
            component.setContainerId(snapContainer.getId());
            componentService.save(component);
        }
        componentId = "snap-ndvi";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = SNAPDemo.ndvi();
            component.setContainerId(snapContainer.getId());
            componentService.save(component);
        }
        componentId = "snap-msavi";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = SNAPDemo.msavi();
            component.setContainerId(snapContainer.getId());
            componentService.save(component);
        }
        componentId = "snap-resample";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = SNAPDemo.resample();
            component.setContainerId(snapContainer.getId());
            componentService.save(component);
        }

        // let's have a DataSourceComponent
        componentId = "Sentinel2-Amazon Web Services";
        DataSourceComponent dataSourceComponent;
        dataSourceComponent = persistenceManager.getDataSourceInstance(componentId);
        if (dataSourceComponent == null) {
            dataSourceComponent = new DataSourceComponent("Sentinel2", "Amazon Web Services");
            dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
            dataSourceComponent.setLabel(dataSourceComponent.getSensorName() + " from " + dataSourceComponent.getDataSourceName());
            dataSourceComponent.setVersion("1.0");
            dataSourceComponent.setDescription(dataSourceComponent.getId());
            dataSourceComponent.setAuthors("TAO Team");
            dataSourceComponent.setCopyright("(C) TAO Team");
            dataSourceComponent.setNodeAffinity("Any");
            persistenceManager.saveDataSourceComponent(dataSourceComponent);
        }

        // Initialize test workflows
        // Workflow 1: SNAP NDVI -> OTB RESAMPLE
        WorkflowDescriptor descriptor1 = new WorkflowDescriptor();
        descriptor1.setName("SNAP NDVI + OTB RESAMPLE workflow");
        descriptor1.setStatus(Status.DRAFT);
        descriptor1.setCreated(LocalDateTime.now());
        descriptor1.setActive(true);
        descriptor1.setUserName("admin");
        descriptor1.setVisibility(Visibility.PRIVATE);
        descriptor1.setCreated(LocalDateTime.now());
        addNodes1(descriptor1);
        persistenceManager.updateWorkflowDescriptor(descriptor1);

        // Workflow 1: OTB RI -> OTB RESAMPLE
        WorkflowDescriptor descriptor2 = new WorkflowDescriptor();
        descriptor2.setName("OTB Radiometric Indices + OTB RESAMPLE workflow");
        descriptor2.setStatus(Status.DRAFT);
        descriptor2.setCreated(LocalDateTime.now());
        descriptor2.setActive(true);
        descriptor2.setUserName("admin");
        descriptor2.setVisibility(Visibility.PRIVATE);
        descriptor2.setCreated(LocalDateTime.now());
        addNodes2(descriptor2);
        persistenceManager.updateWorkflowDescriptor(descriptor2);

        // Workflow 3: OTB RESAMPLE -> {OTB RI NDVI + OTB RI TNDVI} -> OTB CONCATENATE
        WorkflowDescriptor descriptor3 = new WorkflowDescriptor();
        descriptor3.setName("OTB Resample, NDVI, TNDVI and Concatenate");
        descriptor3.setStatus(Status.DRAFT);
        descriptor3.setCreated(LocalDateTime.now());
        descriptor3.setActive(true);
        descriptor3.setUserName("admin");
        descriptor3.setVisibility(Visibility.PRIVATE);
        descriptor3.setCreated(LocalDateTime.now());
        addNodes3(descriptor3);
        persistenceManager.updateWorkflowDescriptor(descriptor3);

        // Workflow 4: SNAP Resample -> {SNAP NDVI + SNAP MSAVI} -> OTB CONCATENATE
        WorkflowDescriptor descriptor4 = new WorkflowDescriptor();
        descriptor4.setName("SNAP Resample, NDVI, MSAVI and OTB Concatenate");
        descriptor4.setStatus(Status.DRAFT);
        descriptor4.setCreated(LocalDateTime.now());
        descriptor4.setActive(true);
        descriptor4.setUserName("admin");
        descriptor4.setVisibility(Visibility.PRIVATE);
        descriptor4.setCreated(LocalDateTime.now());
        addNodes4(descriptor4);
        persistenceManager.updateWorkflowDescriptor(descriptor4);

        // Workflow 5: Data query + fetch 2 products -> Group { SNAP NDVI + OTB RESAMPLE }
        WorkflowDescriptor descriptor5 = new WorkflowDescriptor();
        descriptor5.setName("AWS Download, { SNAP NDVI and OTB Resample }");
        descriptor5.setStatus(Status.DRAFT);
        descriptor5.setCreated(LocalDateTime.now());
        descriptor5.setActive(true);
        descriptor5.setUserName("admin");
        descriptor5.setVisibility(Visibility.PRIVATE);
        descriptor5.setCreated(LocalDateTime.now());
        addNodes5(descriptor5, dataSourceComponent);
        persistenceManager.updateWorkflowDescriptor(descriptor5);

        return new ResponseEntity<>(new WorkflowDescriptor[]
                { descriptor1, descriptor2, descriptor3, descriptor4, descriptor5 },
                HttpStatus.OK);
    }

    private void addNodes1(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP NDVI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("snap-ndvi");
        //node1.addCustomValue("list", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);
        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("otbcli_RigidTransformResample");
        node2.addCustomValue("transformTypeIdScaleX", "0.5");
        node2.addCustomValue("transformTypeIdScaleY", "0.5");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);
        parent.setNodes(nodes);

        persistenceManager.saveWorkflowDescriptor(parent);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ArrayList<ComponentLink> links = new ArrayList<>();
        ComponentLink link = new ComponentLink(node1.getId(),
                                                component1.getTargets().get(0),
                                                component2.getSources().get(0));
        links.add(link);
        node2.setIncomingLinks(links);
    }

    private void addNodes2(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("OTB RI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("otbcli_RadiometricIndices");
        node1.addCustomValue("list", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);
        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("otbcli_RigidTransformResample");
        node2.addCustomValue("transformTypeIdScaleX", "0.5");
        node2.addCustomValue("transformTypeIdScaleY", "0.5");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);
        parent.setNodes(nodes);
        persistenceManager.saveWorkflowDescriptor(parent);
        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ArrayList<ComponentLink> links = new ArrayList<>();
        ComponentLink link = new ComponentLink(node1.getId(),
                                                component1.getTargets().get(0),
                                                component2.getSources().get(0));
        links.add(link);
        node2.setIncomingLinks(links);
    }

    private void addNodes3(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("OTB Resample");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("otbcli_RigidTransformResample");
        node1.addCustomValue("transformTypeIdScaleX", "0.5");
        node1.addCustomValue("transformTypeIdScaleY", "0.5");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Indices");
        node2.setxCoord(600);
        node2.setyCoord(150);
        node2.setComponentId("otbcli_RadiometricIndices");
        node2.addCustomValue("list", "Vegetation:NDVI");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("OTB Indices");
        node3.setxCoord(600);
        node3.setyCoord(750);
        node3.setComponentId("otbcli_RadiometricIndices");
        node3.addCustomValue("list", "Vegetation:TNDVI");
        node3.setCreated(LocalDateTime.now());
        nodes.add(node3);

        WorkflowNodeDescriptor node4 = new WorkflowNodeDescriptor();
        node4.setWorkflow(parent);
        node4.setName("OTB Concatenate");
        node4.setxCoord(900);
        node4.setyCoord(600);
        node4.setComponentId("otbcli_ConcatenateImages");
        node4.setCreated(LocalDateTime.now());
        nodes.add(node4);

        parent.setNodes(nodes);
        persistenceManager.saveWorkflowDescriptor(parent);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ProcessingComponent component3 = componentService.findById(node3.getComponentId());
        ProcessingComponent component4 = componentService.findById(node4.getComponentId());
        ArrayList<ComponentLink> links1 = new ArrayList<>();
        ComponentLink link1 = new ComponentLink(node1.getId(),
                component1.getTargets().get(0),
                component2.getSources().get(0));
        links1.add(link1);
        node2.setIncomingLinks(links1);

        ArrayList<ComponentLink> links2 = new ArrayList<>();
        ComponentLink link2 = new ComponentLink(node1.getId(),
                component1.getTargets().get(0),
                component3.getSources().get(0));
        links2.add(link2);
        node3.setIncomingLinks(links2);

        ArrayList<ComponentLink> links3 = new ArrayList<>();
        ComponentLink link3 = new ComponentLink(node2.getId(),
                component2.getTargets().get(0),
                component4.getSources().get(0));
        links3.add(link3);
        ComponentLink link4 = new ComponentLink(node3.getId(),
                component3.getTargets().get(0),
                component4.getSources().get(0));
        links3.add(link4);
        node4.setIncomingLinks(links3);
    }

    private void addNodes4(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP Resample");
        node1.setxCoord(300);
        node1.setyCoord(150);
        node1.setComponentId("snap-resample");
        node1.addCustomValue("targetResolution", "60");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("SNAP NDVI");
        node2.setxCoord(500);
        node2.setyCoord(50);
        node2.setComponentId("snap-ndvi");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("SNAP MSAVI");
        node3.setxCoord(500);
        node3.setyCoord(250);
        node3.setComponentId("snap-msavi");
        node3.setCreated(LocalDateTime.now());
        nodes.add(node3);

        WorkflowNodeDescriptor node4 = new WorkflowNodeDescriptor();
        node4.setWorkflow(parent);
        node4.setName("OTB Combine");
        node4.setxCoord(800);
        node4.setyCoord(150);
        node4.setComponentId("otbcli_ConcatenateImages");
        node4.setCreated(LocalDateTime.now());
        nodes.add(node4);

        parent.setNodes(nodes);
        persistenceManager.saveWorkflowDescriptor(parent);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ProcessingComponent component3 = componentService.findById(node3.getComponentId());
        ProcessingComponent component4 = componentService.findById(node4.getComponentId());
        ArrayList<ComponentLink> links1 = new ArrayList<>();
        ComponentLink link1 = new ComponentLink(node1.getId(),
                component1.getTargets().get(0),
                component2.getSources().get(0));
        links1.add(link1);
        node2.setIncomingLinks(links1);

        ArrayList<ComponentLink> links2 = new ArrayList<>();
        ComponentLink link2 = new ComponentLink(node1.getId(),
                component1.getTargets().get(0),
                component3.getSources().get(0));
        links2.add(link2);
        node3.setIncomingLinks(links2);

        ArrayList<ComponentLink> links3 = new ArrayList<>();
        ComponentLink link3 = new ComponentLink(node2.getId(),
                component2.getTargets().get(0),
                component4.getSources().get(0));
        links3.add(link3);
        ComponentLink link4 = new ComponentLink(node3.getId(),
                component3.getTargets().get(0),
                component4.getSources().get(0));
        links3.add(link4);
        node4.setIncomingLinks(links3);
    }

    private void addNodes5(WorkflowDescriptor parent, DataSourceComponent component) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor dsNode = new WorkflowNodeDescriptor();
        dsNode.setWorkflow(parent);
        dsNode.setName("AWS Download");
        dsNode.setxCoord(300);
        dsNode.setyCoord(500);
        dsNode.setComponentId(component.getId());
        dsNode.setCreated(LocalDateTime.now());
        parent.addNode(dsNode);

        WorkflowNodeGroupDescriptor grpNode = new WorkflowNodeGroupDescriptor();
        grpNode.setWorkflow(parent);
        grpNode.setName("Group-1");
        grpNode.setxCoord(100);
        grpNode.setyCoord(100);
        grpNode.setCreated(LocalDateTime.now());
        parent.addNode(grpNode);

        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("SNAP NDVI");
        node1.setxCoord(300);
        node1.setyCoord(500);
        node1.setComponentId("snap-ndvi");
        node1.setCreated(LocalDateTime.now());
        parent.addNode(node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("OTB Resample");
        node2.setxCoord(600);
        node2.setyCoord(500);
        node2.setComponentId("otbcli_RigidTransformResample");
        node2.addCustomValue("transformTypeIdScaleX", "0.5");
        node2.addCustomValue("transformTypeIdScaleY", "0.5");
        node2.setCreated(LocalDateTime.now());
        parent.addNode(node2);

        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());

        GroupComponent groupComponent = GroupComponent.create(component1.getSources(), component.getTargetCardinality(),
                                                              component2.getTargets(), component.getTargetCardinality());
        groupComponent = groupComponentService.save(groupComponent);
        grpNode.setComponentId(groupComponent.getId());
        grpNode.addNode(node1);
        grpNode.addNode(node2);

        persistenceManager.saveWorkflowDescriptor(parent);

        ArrayList<ComponentLink> external = new ArrayList<>();
        TargetDescriptor target1 = component.getTargets().get(0);
        external.add(new ComponentLink(dsNode.getId(),
                                       target1, groupComponent.getSources().get(0)));
        grpNode.setIncomingLinks(external);

        ArrayList<ComponentLink> internalLinks = new ArrayList<>();
        TargetDescriptor target2 = component1.getTargets().get(0);
        internalLinks.add(new ComponentLink(node1.getId(),
                                            target2, component2.getSources().get(0)));
        node2.setIncomingLinks(internalLinks);

        persistenceManager.updateWorkflowNodeDescriptor(grpNode);
        persistenceManager.updateWorkflowNodeDescriptor(node2);

        Query dsQuery = new Query();
        dsQuery.setUserId(SystemPrincipal.instance().getName());
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
}
