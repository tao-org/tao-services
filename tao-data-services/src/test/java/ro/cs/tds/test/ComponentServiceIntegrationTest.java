package ro.cs.tds.test;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.services.entity.DataServicesLauncher;

/**
 * Created by cosmin on 11/24/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DataServicesLauncher.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ComponentServiceIntegrationTest extends AbstractServiceIntegrationTest<ProcessingComponent> {

    private static final String componentId = "segmentation-cc-1";
    private static final String componentLabel = "First segmentation component";
    private static final String urlMappingStr = "/component/";

    public ComponentServiceIntegrationTest() {
        super(ProcessingComponent.class);
    }

    protected String getUrlMappingStr()
    {
        return urlMappingStr;
    }
    protected String getTestItemId()
    {
        return componentId;
    }

    protected String getTestItemJson()
    {
        return testComponentJson;
    }
    protected boolean checkItem(ProcessingComponent item, boolean isActive)
    {
        if (componentId.equals(item.getId()) && componentLabel.equals(item.getLabel()) &&
                item.getActive() == isActive) {
            return true;
        }
        return false;
    }

    private static final String testComponentJson =
            "{" +
                    "    \"@type\": \"ProcessingComponent\"," +
                    "    \"id\": \"" + componentId + "\"," +
                    "    \"label\": \"" + componentLabel + "\"," +
                    "    \"version\": \"1.0\"," +
                    "    \"description\": \"Performs segmentation of an image, and output either a raster or a vector file. In vector mode, large input datasets are supported.\"," +
                    "    \"authors\": \"King Arthur\"," +
                    "    \"copyright\": \"(C) Camelot Productions\"," +
                    "    \"nodeAffinity\": \"Any\"," +
                    "    \"sources\": [{" +
                    "        \"id\": \"sourceProductFile\"," +
                    "        \"data\": null," +
                    "        \"constraints\": [\"ro.cs.tao.component.constraints.RasterConstraint\"]" +
                    "    }]," +
                    "    \"targets\": [{" +
                    "        \"id\": \"out_str\"," +
                    "        \"data\": null," +
                    "        \"constraints\": []" +
                    "    }]," +
                    "    \"containerId\": \"DummyTestDockerContainer\"," +
                    "    \"fileLocation\": \"E:\\\\OTB\\\\otbcli_Segmentation.bat\"," +
                    "    \"workingDirectory\": \"E:\\\\OTB\"," +
                    "    \"templateType\": \"VELOCITY\"," +
                    "    \"templateEngine\": {" +
                    "        \"type\": \"ro.cs.tao.component.template.engine.VelocityTemplateEngine\"," +
                    "        \"templateType\": \"VELOCITY\"" +
                    "    }," +
                    "    \"template\": {" +
                    "        \"type\": \"ro.cs.tao.component.template.BasicTemplate\"," +
                    "        \"name\": \"segmentation-cc-template.vm\"," +
                    "        \"templateType\": \"VELOCITY\"," +
                    "        \"contents\": \"-in $sourceProductFile -filter.cc.expr $expr_string -mode.vector.out $out_str -mode.vector.outmode $outmode_string -mode.vector.neighbor $neighbor_bool -mode.vector.stitch $stitch_bool -mode.vector.minsize $minsize_int -mode.vector.simplify $simplify_float -mode.vector.layername $layername_string -mode.vector.fieldname $fieldname_string -mode.vector.tilesize $tilesize_int -mode.vector.startlabel $startlabel_int\"" +
                    "    }," +
                    "    \"variables\": [{" +
                    "        \"key\": \"ITK_AUTOLOAD_PATH\"," +
                    "        \"value\": \"E:\\\\OTB\\bin\"" +
                    "    }]," +
                    "    \"multiThread\": false," +
                    "    \"visibility\": 2," +
                    "    \"active\": true," +
                    "    \"parameterDescriptors\": [{" +
                    "        \"id\": \"outmode_string\"," +
                    "        \"type\": 1," +
                    "        \"dataType\": \"string\"," +
                    "        \"defaultValue\": \"ulco\"," +
                    "        \"description\": \"This allows setting the writing behaviour for the output vector file. Please note that the actual behaviour depends on the file format.\"," +
                    "        \"label\": \"outmode_string\"," +
                    "        \"unit\": null," +
                    "        \"valueSet\": null," +
                    "        \"format\": null," +
                    "        \"notNull\": false," +
                    "        \"validator\": null" +
                    "    }, {" +
                    "        \"id\": \"neighbor_bool\"," +
                    "        \"type\": 1," +
                    "        \"dataType\": \"bool\"," +
                    "        \"defaultValue\": \"true\"," +
                    "        \"description\": \"Activate 8-Neighborhood connectivity (default is 4).\"," +
                    "        \"label\": \"neighbor_bool\"," +
                    "        \"unit\": null," +
                    "        \"valueSet\": null," +
                    "        \"format\": null," +
                    "        \"notNull\": false," +
                    "        \"validator\": null" +
                    "    }]" +
                    "}";
}