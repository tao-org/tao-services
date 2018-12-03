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

public class SampleMultiSourceGroupWorkflow extends SampleWorkflowBase {

    @Override
    public String getName() { return "Local Database Sentinel-2, { SNAP NDVI, SNAP RVI, OTB Resample }, OTB Concatenate"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        DataSourceComponent dataSourceComponent = newDataSourceComponent("Sentinel2", "Local Database");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "Local DB", dataSourceComponent.getId(), ComponentType.DATASOURCE, null,
                                               null, null, null);
        Query dsQuery = new Query();
        dsQuery.setLabel(String.format("Query for sample workflow %d", workflow.getId()));
        dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor(dataSourceComponent.getSensorName());
        dsQuery.setDataSource(dataSourceComponent.getDataSourceName());
        dsQuery.setPageNumber(1);
        dsQuery.setPageSize(10);
        dsQuery.setLimit(2);
        Map<String, String> values = new HashMap<>();
        values.put("acquisition_date", "[2017-04-08,2017-04-10]");
        values.put("geometry", "POLYGON(44.22597512802537 23.50357106719914,44.18762158427181 24.8757389462752," +
                "43.201211355403785 24.8126316594684,43.238273156784096 23.46273547769195," +
                "44.22597512802537 23.50357106719914,44.22597512802537 23.50357106719914))");
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(node1.getId());
        persistenceManager.saveQuery(dsQuery);

        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndviop", ComponentType.PROCESSING, null,
                                               node1, ComponentType.DATASOURCE, Direction.TOP_RIGHT);
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "SNAP RVI", "snap-rviop", ComponentType.PROCESSING, null,
                                               node1, ComponentType.DATASOURCE, Direction.RIGHT);

        customValues.put("transform_type_id_scalex_number", "0.5");
        customValues.put("transform_type_id_scaley_number", "0.5");
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "OTB Resample", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                                               node2, ComponentType.PROCESSING, Direction.RIGHT);
        WorkflowNodeDescriptor node5 = addNode(workflow,
                                               "OTB Resample 2", "RigidTransformResample", ComponentType.PROCESSING, customValues,
                                               node3, ComponentType.PROCESSING, Direction.RIGHT);
        WorkflowNodeDescriptor groupNode = addGroupNode(workflow, "Group-3", node1, node2, node3, node4, node5);
        addNode(workflow, "OTB Concatenate", "ConcatenateImages", ComponentType.PROCESSING, null,
                groupNode, ComponentType.GROUP, Direction.RIGHT);
    }
}
