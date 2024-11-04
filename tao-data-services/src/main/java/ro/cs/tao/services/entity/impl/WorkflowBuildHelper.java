package ro.cs.tao.services.entity.impl;

import ro.cs.tao.component.NodeAffinity;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.QueryService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.utils.WorkflowUtilities;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;
import ro.cs.tao.workflow.enums.Status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

public class WorkflowBuildHelper {

    private static WorkflowService workflowService;
    private static DataSourceComponentService dataSourceService;
    private static QueryService queryService;
    private static ComponentService componentService;
    private static final float xOrigin = 300;
    private static final float yOrigin = 150;
    private static final float xStep = 300;
    private static final float yStep = 150;

    private WorkflowDescriptor workflow;

    public WorkflowBuildHelper(String workflowName) throws PersistenceException {
        this.workflow = workflowService.getDescriptor(workflowName);
        if (this.workflow == null) {
            this.workflow = new WorkflowDescriptor();
        }
        this.workflow.setName(workflowName);
        this.workflow.setStatus(Status.DRAFT);
        this.workflow.setCreated(LocalDateTime.now());
        this.workflow.setActive(true);
        this.workflow.setUserId(SessionStore.currentContext().getPrincipal().getName());
        this.workflow.setVisibility(Visibility.PRIVATE);
        this.workflow = workflowService.save(this.workflow);
    }

    public static void setWorkflowService(WorkflowService service) {
        WorkflowBuildHelper.workflowService = service;
    }

    public static void setComponentService(ComponentService componentService) {
        WorkflowBuildHelper.componentService = componentService;
    }

    public static void setDataSourceService(DataSourceComponentService dataSourceService) {
        WorkflowBuildHelper.dataSourceService = dataSourceService;
    }

    public static void setQueryService(QueryService queryService) {
        WorkflowBuildHelper.queryService = queryService;
    }

    public WorkflowDescriptor getWorkflow() { return workflow; }

    public WorkflowNodeDescriptor addSource(String name, String sensor, String dataSource, Map<String, String> values) throws PersistenceException {
        String componentId = sensor + "-" + dataSource;
        DataSourceComponent dataSourceComponent;
        dataSourceComponent = dataSourceService.findById(componentId);
        if (dataSourceComponent == null) {
            dataSourceComponent = new DataSourceComponent(sensor, dataSource);
            dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
            dataSourceComponent.setLabel(dataSourceComponent.getSensorName() + " from " + dataSourceComponent.getDataSourceName());
            dataSourceComponent.setVersion("1.0");
            dataSourceComponent.setDescription(dataSourceComponent.getId());
            dataSourceComponent.setAuthors("TAO Team");
            dataSourceComponent.setCopyright("(C) TAO Team");
            dataSourceComponent.setNodeAffinity(NodeAffinity.Any);
            dataSourceService.save(dataSourceComponent);
        }
        WorkflowNodeDescriptor dsNode = addNode(name, dataSourceComponent.getId(), ComponentType.DATASOURCE, null, null, null, null);
        Query dsQuery = new Query();
        dsQuery.setUserId(SessionStore.currentContext().getPrincipal().getName());
        dsQuery.setSensor(dataSourceComponent.getSensorName());
        dsQuery.setDataSource(dataSourceComponent.getDataSourceName());
        if (values.containsKey("pageNumber")) {
            dsQuery.setPageNumber(Integer.parseInt(values.get("pageNumber")));
            values.remove("pageNumber");
        } else {
            dsQuery.setPageNumber(1);
        }
        if (values.containsKey("pageSize")) {
            dsQuery.setPageSize(Integer.parseInt(values.get("pageSize")));
            values.remove("pageSize");
        } else {
            dsQuery.setPageSize(10);
        }
        if (values.containsKey("limit")) {
            dsQuery.setLimit(Integer.parseInt(values.get("limit")));
            values.remove("limit");
        } else {
            dsQuery.setLimit(1);
        }
        dsQuery.setValues(values);
        dsQuery.setWorkflowNodeId(dsNode.getId());
        queryService.save(dsQuery);
        return dsNode;
    }

    public WorkflowNodeDescriptor addNode(String name, String componentId, ComponentType componentType, Map<String, String> customValues,
                                          WorkflowNodeDescriptor parentNode, ComponentType parentComponentType,
                                          Direction relativeDirection) throws PersistenceException {
        WorkflowNodeDescriptor node = new WorkflowNodeDescriptor();
        node.setWorkflow(this.workflow);
        node.setName(name);
        float[] coords = placeNode(parentNode, relativeDirection);
        node.setxCoord(coords[0]);
        node.setyCoord(coords[1]);
        node.setComponentId(componentId);
        node.setComponentType(componentType);
        if (customValues != null) {
            for (Map.Entry<String, String> entry : customValues.entrySet()) {
                node.addCustomValue(entry.getKey(), entry.getValue());
            }
        }
        node.setPreserveOutput(true);
        node.setCreated(LocalDateTime.now());
        node = workflowService.addNode(this.workflow.getId(), node);
        if (parentNode != null) {
            TaoComponent component1 = WorkflowUtilities.findComponent(parentNode);
            TaoComponent component2 = WorkflowUtilities.findComponent(node);
            workflowService.addLink(parentNode.getId(), component1.getTargets().get(0).getId(),
                                    node.getId(), component2.getSources().get(0).getId());
        }
        return node;
    }

    public WorkflowNodeDescriptor addGroupNode(String name, WorkflowNodeDescriptor parentNode,
                                               WorkflowNodeDescriptor... nodes) throws PersistenceException {
        WorkflowNodeGroupDescriptor grpNode = new WorkflowNodeGroupDescriptor();
        grpNode.setWorkflow(this.workflow);
        grpNode.setName(name);
        float[] coords = placeNode(parentNode, Direction.RIGHT);
        grpNode.setxCoord(coords[0]);
        grpNode.setyCoord(coords[1]);
        grpNode.setCreated(LocalDateTime.now());
        grpNode.setPreserveOutput(true);
        return workflowService.group(this.workflow.getId(), grpNode, Arrays.asList(nodes));
    }

    public void addLink(WorkflowNodeDescriptor parent, WorkflowNodeDescriptor child) throws PersistenceException {
        if (parent != null && child != null) {
            ProcessingComponent component1 = componentService.findById(parent.getComponentId());
            ProcessingComponent component2 = componentService.findById(child.getComponentId());
            workflowService.addLink(parent.getId(), component1.getTargets().get(0).getId(),
                                    child.getId(), component2.getSources().get(0).getId());
        }
    }

    private float[] placeNode(WorkflowNodeDescriptor relativeTo, Direction direction) {
        float x,y;
        if (relativeTo == null) {
            x = xOrigin;
            y = yOrigin;
        } else {
            x = relativeTo.getxCoord();
            y = relativeTo.getyCoord();
        }
        if (direction != null) {
            switch (direction) {
                case TOP:
                    y -= yStep;
                    break;
                case BOTTOM:
                    y += yStep;
                    break;
                case LEFT:
                    x -= xStep;
                    break;
                case RIGHT:
                    x += xStep;
                    break;
                case TOP_LEFT:
                    x -= xStep;
                    y -= yStep;
                    break;
                case TOP_RIGHT:
                    x += xStep;
                    y -= yStep;
                    break;
                case BOTTOM_LEFT:
                    x -= xStep;
                    y += yStep;
                    break;
                case BOTTOM_RIGHT:
                    x += xStep;
                    y += yStep;
                    break;
            }
        }
        return new float[] { x, y };
    }

    public enum Direction {
        TOP,
        LEFT,
        BOTTOM,
        RIGHT,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
