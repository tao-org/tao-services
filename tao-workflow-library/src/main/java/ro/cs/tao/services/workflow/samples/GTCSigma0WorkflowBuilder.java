package ro.cs.tao.services.workflow.samples;

import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class GTCSigma0WorkflowBuilder extends WorkflowBuilderBase {

    @Override
    public String getName() { return "Geocoded Terrain Corrected Sigma0"; }

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
                                               null, null, null);
        customValues.clear();
        customValues.put("saveDEM", "true");
        customValues.put("applyRadiometricNormalization", "true");
        customValues.put("saveSigmaNought", "true");
        WorkflowNodeDescriptor node2 = addNode(workflow,
                                               "Terrain Correction", "snap-terrain-correction", ComponentType.PROCESSING, customValues,
                                               node1, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.clear();
        customValues.put("exp_str", "abs(im1b1)");
        addNode(workflow,
                "Band Math (ABS)", "BandMath", ComponentType.PROCESSING, customValues,
                node2, ComponentType.PROCESSING, Direction.RIGHT);

    }
}
