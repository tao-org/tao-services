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

public class SampleWorkflow2 extends SampleWorkflowBase {
    @Override
    public String getName() { return "OTB Radiometric Indices + OTB RESAMPLE workflow"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("list_str", "Vegetation:RVI");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "OTB RI", "RadiometricIndices", ComponentType.PROCESSING, customValues,
                                               null, null, null);
        customValues.clear();
        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        addNode(workflow,
                "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                node1, ComponentType.PROCESSING, Direction.RIGHT);
    }
}
