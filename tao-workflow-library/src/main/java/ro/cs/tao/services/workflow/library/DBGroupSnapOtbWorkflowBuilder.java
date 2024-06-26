package ro.cs.tao.services.workflow.library;

import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class DBGroupSnapOtbWorkflowBuilder extends WorkflowBuilderBase {
    @Override
    public String getName() { return "Local DB, { SNAP NDVI, OTB Resample}, OTB Concatenate "; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        DataSourceComponent dataSourceComponent = newDataSourceComponent("Sentinel2", "Local Database");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "Local DB", dataSourceComponent.getId(), ComponentType.DATASOURCE, null,
                                               null, null, (Direction) null);
        Query dsQuery = new Query();
        dsQuery.setLabel(String.format("Query for sample workflow %d", workflow.getId()));
        dsQuery.setUserId(workflow.getUserId());
        dsQuery.setSensor(dataSourceComponent.getSensorName());
        dsQuery.setDataSource(dataSourceComponent.getDataSourceName());
        dsQuery.setPageNumber(1);
        dsQuery.setPageSize(10);
        dsQuery.setLimit(2);
        Map<String, String> values = new HashMap<>();
        values.put(CommonParameterNames.START_DATE, "[2017-04-08,2017-04-10]");
        values.put(CommonParameterNames.FOOTPRINT, "POLYGON((23.50357106719914 44.22597512802537,24.8757389462752 44.18762158427181," +
                "24.8126316594684 43.201211355403785,23.46273547769195 43.238273156784096," +
                "23.50357106719914 44.22597512802537,23.50357106719914 44.22597512802537))");
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(node1.getId());
        persistenceManager.queries().save(dsQuery);

        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndviop", ComponentType.PROCESSING, null,
                                               node1, ComponentType.DATASOURCE, Direction.TOP_RIGHT);
        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                                               node2, ComponentType.PROCESSING, Direction.RIGHT);
        WorkflowNodeDescriptor groupNode = addGroupNode(workflow, "Group-2", node1, node2, node3);
        addNode(workflow, "OTB Concatenate", "ConcatenateImages", ComponentType.PROCESSING, null,
                groupNode, ComponentType.GROUP, Direction.RIGHT);
    }
}