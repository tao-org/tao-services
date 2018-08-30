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

package ro.cs.tao.services.workflow.samples;

import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.base.SampleWorkflowBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class SampleWorkflow5 extends SampleWorkflowBase {
    @Override
    public String getName() { return "AWS Download, { SNAP NDVI and OTB Resample }"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        DataSourceComponent dataSourceComponent = newDataSourceComponent("Sentinel2", "Amazon Web Services");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "AWS Download", dataSourceComponent.getId(), ComponentType.DATASOURCE, null,
                                               null, null, null);
        Query dsQuery = new Query();
        dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor(dataSourceComponent.getSensorName());
        dsQuery.setDataSource(dataSourceComponent.getDataSourceName());
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
        dsQuery.setWorkflowNodeId(node1.getId());
        persistenceManager.saveQuery(dsQuery);

        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndvi", ComponentType.PROCESSING, null,
                                               null, null, null);
        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                                               node2, ComponentType.PROCESSING, Direction.RIGHT);
        addGroupNode(workflow, "Group-1", node1, node2, node3);
    }
}
