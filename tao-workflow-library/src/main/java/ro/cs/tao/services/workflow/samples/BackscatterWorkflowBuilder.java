package ro.cs.tao.services.workflow.samples;

import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.base.WorkflowBuilderBase;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class BackscatterWorkflowBuilder extends WorkflowBuilderBase {

    @Override
    public String getName() { return "Compute Sentinel-1 Backscatter"; }

    @Override
    protected void addNodes(WorkflowDescriptor workflow) throws PersistenceException {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("orbitType", "Sentinel Precise (Auto Download)");
        customValues.put("polyDegree", "3");
        customValues.put("continueOnFail", "true");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "apply_orbit_file_intermediate1.dim");
        WorkflowNodeDescriptor orbitFileNode1 = addNode(workflow,
                                               "Apply Orbit File 1", "snap-apply-orbit-file", ComponentType.PROCESSING, customValues,
                                               null, null, null);
        customValues.put("t", "apply_orbit_file_intermediate2.dim");
        WorkflowNodeDescriptor orbitFileNode2 = addNode(workflow,
                                                        "Apply Orbit File 2", "snap-apply-orbit-file", ComponentType.PROCESSING, customValues,
                                                        null, null, Direction.BOTTOM);
        customValues.clear();
        customValues.put("auxFile", "Latest Auxiliary File");
        customValues.put("outputImageInComplex", "true");
        customValues.put("outputImageScaleInDb", "false");
        customValues.put("createGammaBand", "false");
        customValues.put("createBetaBand", "false");
        customValues.put("outputSigmaBand", "true");
        customValues.put("outputGammaBand", "false");
        customValues.put("outputBetaBand", "false");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "calibration_intermediate1.dim");
        WorkflowNodeDescriptor calibrationNode1 = addNode(workflow,
                                                          "Calibration 1", "snap-calibration", ComponentType.PROCESSING, customValues,
                                                          orbitFileNode1, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("t", "calibration_intermediate2.dim");
        WorkflowNodeDescriptor calibrationNode2 = addNode(workflow,
                                                          "Calibration 2", "snap-calibration", ComponentType.PROCESSING, customValues,
                                                          orbitFileNode2, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.clear();
        customValues.put("subswath","IW1");
        customValues.put("selectedPolarisations","VH");
        customValues.put("firstBurstIndex", "1");
        customValues.put("lastBurstIndex", "9");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "topsar_split_intermediate11.dim");
        WorkflowNodeDescriptor splitNode11 = addNode(workflow,
                                                     "TOPSAR-Split 1-1", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode1, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        customValues.put("t", "topsar_split_intermediate21.dim");
        WorkflowNodeDescriptor splitNode21 = addNode(workflow,
                                                     "TOPSAR-Split 2-1", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode2, ComponentType.PROCESSING, Direction.TOP_RIGHT);
        customValues.put("subswath","IW2");
        customValues.put("t", "topsar_split_intermediate12.dim");
        WorkflowNodeDescriptor splitNode12 = addNode(workflow,
                                                     "TOPSAR-Split 1-2", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode1, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("t", "topsar_split_intermediate22.dim");
        WorkflowNodeDescriptor splitNode22 = addNode(workflow,
                                                     "TOPSAR-Split 2-2", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode2, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("subswath","IW3");
        customValues.put("t", "topsar_split_intermediate13.dim");
        WorkflowNodeDescriptor splitNode13 = addNode(workflow,
                                                     "TOPSAR-Split 1-3", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode1, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        customValues.put("t", "topsar_split_intermediate13.dim");
        WorkflowNodeDescriptor splitNode23 = addNode(workflow,
                                                     "TOPSAR-Split 3-3", "snap-topsar-split", ComponentType.PROCESSING, customValues,
                                                     calibrationNode2, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        customValues.clear();
        customValues.put("demName", "SRTM 3sec");
        customValues.put("demResamplingMethod", "BICUBIC_INTERPOLATION");
        customValues.put("externalDEMNoDataValue", "0.0");
        customValues.put("resamplingType", "BISINC_5_POINT_INTERPOLATION");
        customValues.put("maskOutAreaWithoutElevation", "true");
        customValues.put("outputRangeAzimuthOffset", "false");
        customValues.put("outputDerampDemodPhase", "false");
        customValues.put("disableReramp", "false");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "back_geocoding_1.dim");
        WorkflowNodeDescriptor geocoding1 = addNode(workflow,
                                                    "Back-Geocoding 1", "snap-back-geocoding", ComponentType.PROCESSING, customValues,
                                                    splitNode11, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(workflow, splitNode21, geocoding1);
        customValues.put("t", "back_geocoding_2.dim");
        WorkflowNodeDescriptor geocoding2 = addNode(workflow,
                                                    "Back-Geocoding 2", "snap-back-geocoding", ComponentType.PROCESSING, customValues,
                                                    splitNode12, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(workflow, splitNode22, geocoding2);
        customValues.put("t", "back_geocoding_3.dim");
        WorkflowNodeDescriptor geocoding3 = addNode(workflow,
                                                    "Back-Geocoding 3", "snap-back-geocoding", ComponentType.PROCESSING, customValues,
                                                    splitNode13, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(workflow, splitNode23, geocoding3);
        customValues.clear();
        customValues.put("selectedPolarisations", "VH");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "deburst_1.dim");
        WorkflowNodeDescriptor deburst1 = addNode(workflow,
                                                  "TOPSAR Deburst 1", "snap-topsar-deburst", ComponentType.PROCESSING, customValues,
                                                  geocoding1, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("t", "deburst_2.dim");
        WorkflowNodeDescriptor deburst2 = addNode(workflow,
                                                  "TOPSAR Deburst 2", "snap-topsar-deburst", ComponentType.PROCESSING, customValues,
                                                  geocoding2, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("t", "deburst_3.dim");
        WorkflowNodeDescriptor deburst3 = addNode(workflow,
                                                  "TOPSAR Deburst 3", "snap-topsar-deburst", ComponentType.PROCESSING, customValues,
                                                  geocoding3, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.put("t", "backscatter_raw.dim");
        WorkflowNodeDescriptor backscatterRaw = addNode(workflow,
                                                        "TOPSAR-Merge", "snap-topsar-merge", ComponentType.PROCESSING, customValues,
                                                        deburst1, ComponentType.PROCESSING, Direction.BOTTOM_RIGHT);
        addLink(workflow, deburst2, backscatterRaw);
        addLink(workflow, deburst3, backscatterRaw);
        customValues.clear();
        customValues.put("nRgLooks", "4");
        customValues.put("nAzLooks", "1");
        customValues.put("outputIntensity", "false");
        customValues.put("grSquarePixel", "true");
        customValues.put("formatName", "BEAM-DIMAP");
        customValues.put("t", "multilook.dim");
        WorkflowNodeDescriptor multilook = addNode(workflow,
                                                   "Multilook", "snap-multilook", ComponentType.PROCESSING, customValues,
                                                   backscatterRaw, ComponentType.PROCESSING, Direction.RIGHT);
        customValues.clear();
        customValues.put("pixelSpacingInMeter", "20.0");
        customValues.put("pixelSpacingInDegree", "1.796630568239043E-4");
        customValues.put("mapProjection", "GEOGCS[&quot;WGS84(DD)&quot;, \n" +
                "  DATUM[&quot;WGS84&quot;, \n" +
                "    SPHEROID[&quot;WGS84&quot;, 6378137.0, 298.257223563]], \n" +
                "  PRIMEM[&quot;Greenwich&quot;, 0.0], \n" +
                "  UNIT[&quot;degree&quot;, 0.017453292519943295], \n" +
                "  AXIS[&quot;Geodetic longitude&quot;, EAST], \n" +
                "  AXIS[&quot;Geodetic latitude&quot;, NORTH]]");
        customValues.put("formatName", "GeoTIFF");
        customValues.put("t", "backscatter.tif");
        addNode(workflow,
                "Terrain Correction", "snap-terrain-correction", ComponentType.PROCESSING, customValues,
                multilook, ComponentType.PROCESSING, Direction.RIGHT);

    }
}
