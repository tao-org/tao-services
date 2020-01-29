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

public class OtbTwoSnapOtbWorkflowBuilder extends WorkflowBuilderBase {
    @Override
    public String getName() { return "OTB Resample, NDVI, TNDVI and Concatenate"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                                               null, null, (Direction) null);
        customValues.clear();
        customValues.put("list_str", "Vegetation:NDVI");
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "OTB NDVI", "RadiometricIndices", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        customValues.clear();
        customValues.put("list_str", "Vegetation:TNDVI");
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "OTB TNDVI", "RadiometricIndices", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "OTB Concatenate", "ConcatenateImages", ComponentType.PROCESSING, null,
                                               node2, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(node3, node4);
    }
}
