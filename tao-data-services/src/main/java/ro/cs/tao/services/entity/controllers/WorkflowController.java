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
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.entity.impl.ComponentServiceImpl;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.WorkflowDescriptor;

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
            container.setName("TestContainer");
            container.setTag("For test puproses only");
            container.setApplicationPath("/usr/bin/apppath/");
            container = persistenceManager.saveContainer(container);
        }
        List<Application> applications = container.getApplications();
        if (applications == null || applications.size() == 0) {
            for (int i = 1; i <= 6; i++) {
                Application application = new Application();
                application.setName("segmentation-cc-" + String.valueOf(i));
                application.setPath("/usr/bin/apppath");
                container.addApplication(application);
            }
            persistenceManager.updateContainer(container);
        }
        // Initialize Processing components
        for (int i = 1; i <= 6; i++) {
            ProcessingComponent component = ComponentServiceImpl.newComponent("segmentation-cc-" + String.valueOf(i),
                    String.valueOf(i) + (i == 1 ? "st " : i == 2 ? "nd " : i == 3 ? "rd " : "th ") + "component");
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
        WorkflowDescriptor descriptor = workflowService.findById("1");
        persistenceManager.saveWorkflowDescriptor(descriptor);
        /*for (WorkflowNodeDescriptor nodeDescriptor : descriptor.getNodes()) {
            persistenceManager.saveWorkflowNodeDescriptor(nodeDescriptor, descriptor);
        }*/

        return new ResponseEntity<>("Initialization completed", HttpStatus.OK);
    }
}
