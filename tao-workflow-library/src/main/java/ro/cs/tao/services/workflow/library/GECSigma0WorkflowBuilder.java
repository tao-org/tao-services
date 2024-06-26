package ro.cs.tao.services.workflow.library;

import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;
import ro.cs.tao.workflow.enums.Status;

import java.util.HashMap;
import java.util.Map;

public class GECSigma0WorkflowBuilder extends WorkflowBuilderBase {

    @Override
    public String getName() { return "Geocoded Ellipsoid Corrected Sigma0"; }

    @Override
    public WorkflowDescriptor createSystemWorkflowDescriptor() throws PersistenceException {
        WorkflowDescriptor descriptor = createWorkflowDescriptor();
        descriptor.setVisibility(Visibility.PUBLIC);
        descriptor.setStatus(Status.PUBLISHED);
        descriptor.setUserId(SystemPrincipal.instance().getName());
        return persistenceManager.workflows().update(descriptor);
    }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("nRgLooks", "3");
        customValues.put("nAzLooks", "3");
        customValues.put("outputIntensity", "true");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "multilook_intermediate.dim");
        WorkflowNodeDescriptor node1 = addNode(workflow,
                                               "Multilook", "snap-multilook", ComponentType.PROCESSING, customValues,
                                               null, null, (Direction) null);
        customValues.clear();
        customValues.put("formatName", "GeoTIFF");
        addNode(workflow,
                "Ellipsoid Correction", "snap-ellipsoid-correction-rd", ComponentType.PROCESSING, customValues,
                node1, ComponentType.PROCESSING, Direction.RIGHT);
    }


}
