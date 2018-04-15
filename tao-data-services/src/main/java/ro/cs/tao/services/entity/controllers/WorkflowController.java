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
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.entity.demo.OTBDemo;
import ro.cs.tao.services.entity.demo.SNAPDemo;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.utils.Platform;
import ro.cs.tao.workflow.Status;
import ro.cs.tao.workflow.Visibility;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                    "C:\\Tools\\OTB-6.4.0\\bin" : "/opt/OTB-6.4.0-Linux64/bin/";
            otbContainer = containerService.initOTB(otbPath);
        }

        // Initialize SNAP test otbContainer
        Container snapContainer = null;
        try {
            snapContainer = persistenceManager.getContainerById("SNAPContainer");
        } catch (PersistenceException ignored) { }
        if (snapContainer == null) {
            snapContainer = new Container();
            snapContainer.setId(UUID.randomUUID().toString());
            snapContainer.setName("SNAPContainer");
            snapContainer.setTag("For test puproses only");
            switch (currentPlatform.getId()) {
                case win:
                    snapContainer.setApplicationPath("");
                    break;
                default:
                    snapContainer.setApplicationPath("/opt/SNAP-6.0.0-Linux64/bin/");
                    break;
            }
            snapContainer = persistenceManager.saveContainer(snapContainer);
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


        return new ResponseEntity<>(new WorkflowDescriptor[] { descriptor1, descriptor2, descriptor3 }, HttpStatus.OK);
    }

    private void addNodes1(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("Node-1");
        node1.setxCoord(100);
        node1.setyCoord(100);
        node1.setComponentId("snap-ndvi");
        //node1.addCustomValue("list", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);
        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("Node-2");
        node2.setxCoord(300);
        node2.setyCoord(100);
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
        node1.setName("Node-3");
        node1.setxCoord(100);
        node1.setyCoord(100);
        node1.setComponentId("otbcli_RadiometricIndices");
        node1.addCustomValue("list", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);
        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("Node-4");
        node2.setxCoord(300);
        node2.setyCoord(100);
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
        node1.setName("Node-5");
        node1.setxCoord(100);
        node1.setyCoord(100);
        node1.setComponentId("otbcli_RigidTransformResample");
        node1.addCustomValue("transformTypeIdScaleX", "0.5");
        node1.addCustomValue("transformTypeIdScaleY", "0.5");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);

        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("Node-6");
        node2.setxCoord(300);
        node2.setyCoord(0);
        node2.setComponentId("otbcli_RadiometricIndices");
        node2.addCustomValue("list", "Vegetation:NDVI");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);

        WorkflowNodeDescriptor node3 = new WorkflowNodeDescriptor();
        node3.setWorkflow(parent);
        node3.setName("Node-7");
        node3.setxCoord(300);
        node3.setyCoord(300);
        node3.setComponentId("otbcli_RadiometricIndices");
        node3.addCustomValue("list", "Vegetation:TNDVI");
        node3.setCreated(LocalDateTime.now());
        nodes.add(node3);

        WorkflowNodeDescriptor node4 = new WorkflowNodeDescriptor();
        node4.setWorkflow(parent);
        node4.setName("Node-8");
        node4.setxCoord(500);
        node4.setyCoord(100);
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
}
