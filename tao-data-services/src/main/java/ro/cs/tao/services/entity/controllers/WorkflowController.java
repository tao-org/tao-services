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
import ro.cs.tao.services.entity.impl.OTBDemo;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.Status;
import ro.cs.tao.workflow.Visibility;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        // Initialize container
        List<Container> containers = containerService.list();
        Container container;
        if (containers != null && containers.size() > 0) {
            container = containers.get(0);
        } else {
            container = new Container();
            container.setName("OTBContainer");
            container.setTag("For test puproses only");
            container.setApplicationPath("C:\\Tools\\OTB-6.4.0\\bin");
            container = persistenceManager.saveContainer(container);
        }
        List<Application> applications = container.getApplications();
        if (applications == null || applications.size() == 0) {
            Application application = new Application();
            application.setName("otb-rigid-transform");
            application.setPath("C:\\Tools\\OTB-6.4.0\\bin");
            container.addApplication(application);
            application = new Application();
            application.setName("otb-radiometric-indices");
            application.setPath("C:\\Tools\\OTB-6.4.0\\bin");
            container.addApplication(application);
            persistenceManager.updateContainer(container);
        }
        // Initialize Processing components
        ProcessingComponent component;
        String componentId = "otb-rigid-transform";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = OTBDemo.rigidTransform();
            componentService.save(component);
        }
        componentId = "otb-radiometric-indices";
        try {
            component = persistenceManager.getProcessingComponentById(componentId);
        } catch (PersistenceException pex) {
            component = OTBDemo.radiometricIndices();
            componentService.save(component);
        }
        // Initialize data source components
        /*DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        SortedSet<String> sensors = dataSourceManager.getSupportedSensors();
        for (String sensor : sensors) {
            List<String> names = dataSourceManager.getNames(sensor);
            for (String name : names) {
                DataSource dataSource = dataSourceManager.get(sensor, name);
                DataSourceComponent dsComponent = new DataSourceComponent(sensor, name);
                dsComponent.setId(sensor + "-" + name);
                dsComponent.setUserName("admin");
                dsComponent.setFetchMode(FetchMode.OVERWRITE);
                dsComponent.setMaxRetries(1);
                dsComponent.setAuthors("TAO Team");
                dsComponent.setDescription(dataSource.getId());
                dsComponent.setLabel("Query Component");
                dsComponent.setVersion("1.0");
                TargetDescriptor targetDescriptor = new TargetDescriptor();
                //targetDescriptor.setName("results");
                DataDescriptor dataDescriptor = new DataDescriptor();
                dataDescriptor.setSensorType(sensor.equals("Sentinel1") ? SensorType.RADAR : SensorType.OPTICAL);
                dataDescriptor.setFormatType(DataFormat.RASTER);
                targetDescriptor.setDataDescriptor(dataDescriptor);
                dsComponent.addTarget(targetDescriptor);
                persistenceManager.saveDataSourceComponent(dsComponent);
            }
        }*/

        // Initialize test workflow
        WorkflowDescriptor descriptor = new WorkflowDescriptor();
        descriptor.setName("Test workflow");
        descriptor.setStatus(Status.DRAFT);
        descriptor.setCreated(LocalDateTime.now());
        descriptor.setActive(true);
        descriptor.setUserName("admin");
        descriptor.setVisibility(Visibility.PRIVATE);
        descriptor.setCreated(LocalDateTime.now());
        addNodes(descriptor);

        persistenceManager.saveWorkflowDescriptor(descriptor);

        return new ResponseEntity<>(descriptor, HttpStatus.OK);
    }

    private void addNodes(WorkflowDescriptor parent) throws PersistenceException {
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        WorkflowNodeDescriptor node1 = new WorkflowNodeDescriptor();
        node1.setWorkflow(parent);
        node1.setName("Node-1");
        node1.setxCoord(100);
        node1.setyCoord(100);
        node1.setComponentId("otb-radiometric-indices");
        node1.addCustomValue("list", "Vegetation:RVI");
        node1.setCreated(LocalDateTime.now());
        nodes.add(node1);
        WorkflowNodeDescriptor node2 = new WorkflowNodeDescriptor();
        node2.setWorkflow(parent);
        node2.setName("Node-2");
        node2.setxCoord(300);
        node2.setyCoord(100);
        node2.setComponentId("otb-rigid-transform");
        node2.addCustomValue("transformTypeIdScaleX", "0.5");
        node2.addCustomValue("transformTypeIdScaleY", "0.5");
        node2.setCreated(LocalDateTime.now());
        nodes.add(node2);
        ProcessingComponent component1 = componentService.findById(node1.getComponentId());
        ProcessingComponent component2 = componentService.findById(node2.getComponentId());
        ArrayList<ComponentLink> links = new ArrayList<>();
        ComponentLink link = new ComponentLink(component1.getTargets().get(0),
                                               component2.getSources().get(0));
        links.add(link);
        node2.setIncomingLinks(links);
        parent.setNodes(nodes);
    }
}
