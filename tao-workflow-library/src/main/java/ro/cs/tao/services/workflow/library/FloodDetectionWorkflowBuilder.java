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

public class FloodDetectionWorkflowBuilder extends WorkflowBuilderBase {
    @Override
    public String getName() { return "Sentinel-1 Flood Detection"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("PauxFile", "Product Auxiliary File");
        customValues.put("PoutputSigmaBand", "false");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "Calibration (1)", "snap-calibration", ComponentType.PROCESSING, customValues,
                                               null, ComponentType.PROCESSING, (Direction) null);
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "Calibration (2)", "snap-calibration", ComponentType.PROCESSING, customValues,
                                               null, ComponentType.PROCESSING, Direction.BOTTOM);
        customValues.clear();
        customValues.put("PresamplingType", "BILINEAR_INTERPOLATION");
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "Stack", "snap-createstack", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        addLink(node2, node3);
        customValues.clear();
        customValues.put("PmaxIteration", "2");
        customValues.put("PinSAROptimized", "false");
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "Cross-correlation", "snap-cross-correlation", ComponentType.PROCESSING, customValues,
                                               node3, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.clear();
        customValues.put("PrmsThreshold", "1.0");
        customValues.put("PinterpolationMethod", "Bilinear interpolation");
        WorkflowNodeDescriptor node5 = addNode(workflow,
                                               "Warp", "snap-warp", ComponentType.PROCESSING, customValues,
                                               node4, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.clear();
        customValues.put("landMask", "false");
        WorkflowNodeDescriptor node6 = addNode(workflow,
                                               "Land-Sea mask", "snap-land-sea-mask", ComponentType.PROCESSING, customValues,
                                               node5, ComponentType.PROCESSING, Direction.RIGHT);
        WorkflowNodeDescriptor node7 = addNode(workflow,
                                               "Terrain mask", "snap-terrain-mask", ComponentType.PROCESSING, null,
                                               node6, ComponentType.PROCESSING, Direction.RIGHT);
        WorkflowNodeDescriptor node8 = addNode(workflow,
                                               "Flood detection", "snap-flood-detection", ComponentType.PROCESSING, null,
                                               node7, ComponentType.PROCESSING, Direction.RIGHT);
    }
}
