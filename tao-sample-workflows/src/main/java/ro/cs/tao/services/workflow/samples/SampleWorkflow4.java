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

import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.base.SampleWorkflowBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class SampleWorkflow4 extends SampleWorkflowBase {
    @Override
    protected String getName() { return "SNAP Resample, NDVI, MSAVI and OTB Concatenate"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("targetResolution", "60");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "SNAP Resample", "snap-resample", ComponentType.PROCESSING, customValues,
                                               null, null, null);
        customValues.clear();
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndvi", ComponentType.PROCESSING, null,
                                               node1, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "SNAP MSAVI", "snap-msavi", ComponentType.PROCESSING, null,
                                               node1, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "OTB Combine", "ConcatenateImages", ComponentType.PROCESSING, null,
                                               node2, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(workflow, node3, node4);
    }
}
