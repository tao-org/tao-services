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

import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapOtbWorkflowBuilder extends WorkflowBuilderBase {

    @Override
    public String getName() { return "Single product SNAP NDVI + OTB RESAMPLE with name rule workflow"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {

        List<String> productNames = new ArrayList<>();
        productNames.add("S2B_MSIL1C_20181226T103439_N0207_R108_T32UMF_20181226T122504");
        DataSourceComponent component = newDataSourceComponent("Sentinel2", productNames, () -> "admin");
        WorkflowNodeDescriptor node1 = addNode(workflow, "Local DB", component.getId(), ComponentType.DATASOURCE, null,
                                               null, null, (Direction) null);
        Map<String, String> customValues = new HashMap<>();
        customValues.put("t", "${1:MISSION}_${1:ADATE}_${1:ORBIT}_NDVI.tif");
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndvi", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.DATASOURCE, Direction.RIGHT);
        customValues.clear();
        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        customValues.put("out", "${1:MISSION}_${1:ADATE}_${1:ORBIT}_NDVI_resampled.tif");
        addNode(workflow,
                "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                node2, ComponentType.PROCESSING, Direction.RIGHT);
    }
}
