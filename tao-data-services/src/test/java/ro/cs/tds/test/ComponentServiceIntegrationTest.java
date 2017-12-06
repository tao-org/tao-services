package ro.cs.tds.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.services.app.ComponentApplication;

import java.io.IOException;

/**
 * Created by cosmin on 11/24/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComponentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ComponentServiceIntegrationTest {

    @LocalServerPort
    private int port;

    TestRestTemplate restTemplate = new TestRestTemplate();


    @Test
    public void testGetTaoComponents() {
        // add the component if does not exists already
        addTaoComponent();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkReceivedComponent(response.getBody(), true, true);
    }

    @Test
    public void testGetSingleTaoComponent() {
        // add the component if does not exists already
        addTaoComponent();

        checkTaoComponentExistence(true);
    }

    @Test
    public void testAddTaoComponent() {
        ResponseEntity<String> response = addTaoComponent();
        checkReceivedComponent(response.getBody(), false, true);
    }

    @Test
    public void deleteTaoComponent() {
        // First add (or update) an active component
        addTaoComponent();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        String fullUrl = createURLWithPort("/component/segmentation-cc-1");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.DELETE, entity, String.class);
        System.out.println(response);

        checkTaoComponentExistence(false);
    }

    private ResponseEntity<String> addTaoComponent() {
        String newCompJson = testComponentJson;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        entity = new HttpEntity<>(newCompJson, headers);
        String fullUrl = createURLWithPort("/component/");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.POST, entity, String.class, newCompJson);
        System.out.println(response);
        return response;
    }

    private void checkTaoComponentExistence(boolean isActive) {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/segmentation-cc-1");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkReceivedComponent(response.getBody(), false, isActive);
    }

    private void checkReceivedComponent(String responseBody, boolean isArray, boolean isActive) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ProcessingComponent[] components = null;
        try {
            if (isArray) {
                components = objectMapper.readValue(responseBody, ProcessingComponent[].class);
            } else {
                components = new ProcessingComponent[]{objectMapper.readValue(responseBody, ProcessingComponent.class)};
            }
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
        for (ProcessingComponent processingComponent: components) {
            if ("segmentation-cc-1".equals(processingComponent.getId()) && "First segmentation component".equals(processingComponent.getLabel()) &&
                    processingComponent.getActive() == isActive) {
                return;
            }
        }
        Assert.assertFalse(true);
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

    private static final String testComponentJson =
            "{" +
            "    \"@type\": \"ProcessingComponent\"," +
            "    \"id\": \"segmentation-cc-1\"," +
            "    \"label\": \"First segmentation component\"," +
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
            "        \"dataType\": \"java.lang.String\"," +
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
            "        \"dataType\": \"java.lang.Boolean\"," +
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