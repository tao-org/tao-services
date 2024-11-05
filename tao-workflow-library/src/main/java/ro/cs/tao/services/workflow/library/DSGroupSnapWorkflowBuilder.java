package ro.cs.tao.services.workflow.library;

import ro.cs.tao.component.NodeAffinity;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DSGroupSnapWorkflowBuilder extends WorkflowBuilderBase {

    @Override
    public String getName() { return "Datasource Group, SNAP NDVI, OTB Concatenate"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        //Map<String, String> customValues = new HashMap<>();
        Principal principal = SessionStore.currentContext().getPrincipal();
        List<String> productNames = new ArrayList<>();
        productNames.add("S2A_MSIL1C_20170731T093041_N0205_R136_T34TFR_20170731T093607");
        DataSourceComponent dataSourceComponent1 =
                dataSourceComponentService.createForLocations(productNames,
                                                              "Sentinel2", "Scientific Data Hub", null,
                                                              "Sample data source 1",
                                                              principal);
        productNames.clear();
        productNames.add("S2A_MSIL1C_20180716T093041_N0206_R136_T34TFR_20180716T114051");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) { }
        DataSourceComponent dataSourceComponent2 =
                dataSourceComponentService.createForLocations(productNames,
                                                              "Sentinel2", "Scientific Data Hub", null,
                                                              "Sample data source 2",
                                                              principal);
        DataSourceComponentGroup group = new DataSourceComponentGroup();
        group.setId("Data Source Group " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        group.setUserId(principal.getName());
        group.setLabel(group.getId());
        group.setVersion("1.0");
        group.setDescription(group.getLabel());
        group.setAuthors("TAO Team");
        group.setCopyright("(C) TAO Team");
        group.setNodeAffinity(NodeAffinity.Any);
        group.addDataSourceComponent(dataSourceComponent1);
        group.addDataSourceComponent(dataSourceComponent2);
        group = persistenceManager.dataSourceGroups().save(group);

        Query query1 = new Query();
        query1.setLabel(String.format("First query for sample workflow %d", workflow.getId()));
        query1.setUserId(principal.getName());
        query1.setSensor(dataSourceComponent1.getSensorName());
        query1.setComponentId(dataSourceComponent1.getId());
        query1.setDataSource("Scientific Data Hub");
        query1.setPageNumber(1);
        query1.setPageSize(10);
        query1.setLimit(1);
        Map<String, String> values = new HashMap<>();
        values.put(CommonParameterNames.START_DATE, "2018-07-15");
        values.put(CommonParameterNames.END_DATE, "2018-07-17");
        values.put(CommonParameterNames.TILE, "34TFR");
        values.put(CommonParameterNames.FOOTPRINT, "POLYGON((23.08888415063469 45.64122237960987,23.497142114745625 45.64122237960987,23.497142114745625 45.884014164289056,23.08888415063469 45.884014164289056,23.08888415063469 45.64122237960987))");
        query1.setValues(values);
        //query1.setWorkflowNodeId(node1.getId());
        query1 = persistenceManager.queries().save(query1);
        group.addQuery(query1, dataSourceComponent1.getSources().get(0).getId());

        Query query2 = new Query();
        query2.setLabel(String.format("Second query for sample workflow %d", workflow.getId()));
        query2.setUserId(principal.getName());
        query2.setSensor(dataSourceComponent2.getSensorName());
        query2.setDataSource("Scientific Data Hub");
        query2.setComponentId(dataSourceComponent2.getId());
        query2.setPageNumber(1);
        query2.setPageSize(10);
        query2.setLimit(1);
        values = new HashMap<>();
        values.put(CommonParameterNames.START_DATE, "2017-07-30");
        values.put(CommonParameterNames.END_DATE, "2017-08-01");
        values.put(CommonParameterNames.TILE, "34TFR");
        values.put(CommonParameterNames.FOOTPRINT, "POLYGON((23.08888415063469 45.64122237960987,23.497142114745625 45.64122237960987,23.497142114745625 45.884014164289056,23.08888415063469 45.884014164289056,23.08888415063469 45.64122237960987))");
        query2.setValues(values);
        //query2.setWorkflowNodeId(node1.getId());
        query2 = persistenceManager.queries().save(query2);
        group.addQuery(query2, dataSourceComponent2.getSources().get(0).getId());
        group = persistenceManager.dataSourceGroups().update(group);

        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "DS Group", group.getId(), ComponentType.DATASOURCE_GROUP, null,
                                               null, null, (Direction) null);
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndviop", ComponentType.PROCESSING, null,
                                               node1, ComponentType.DATASOURCE_GROUP, 0, Direction.TOP_RIGHT);
        WorkflowNodeDescriptor node3 = addNode(workflow,
                                               "SNAP NDVI", "snap-ndviop", ComponentType.PROCESSING, null,
                                               node1, ComponentType.DATASOURCE_GROUP, 1, Direction.BOTTOM_RIGHT);
        WorkflowNodeDescriptor node4 = addNode(workflow,
                                               "OTB Concatenate", "ConcatenateImages", ComponentType.PROCESSING, null,
                                               node2, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(node3, node4);
    }


}
