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

package ro.cs.tao.services.workflow.library;

import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class WaterIndicesWorkflowBuilder extends WorkflowBuilderBase {
    @Override
    public String getName() { return "Sentinel-2 Water Indices"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "NDWI", "snap-ndwi", ComponentType.PROCESSING, null,
                                               null, ComponentType.PROCESSING, (Direction) null);
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "NDPI", "snap-ndpi", ComponentType.PROCESSING, null,
                                               null, ComponentType.PROCESSING, Direction.BOTTOM);
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "NDTI", "snap-ndti", ComponentType.PROCESSING, null,
                                               null, ComponentType.PROCESSING, Direction.BOTTOM);
        customValues.put("t", "${1:MISSION}_${1:TILE}_${1:ADATE}T${1:ATIME}_WATIDX.tif");
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "Combine", "snap-bandmerge", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        addLink(node2, node4);
        addLink(node3, node4);
    }
}
