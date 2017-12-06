package ro.cs.tds.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
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
        addTaoComponent(null, null);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkReceivedComponent(response.getBody());
    }

    @Test
    public void testGetSingleTaoComponent() {
        // add the component if does not exists already
        addTaoComponent(null, null);

        checkTaoComponentExistence();
    }

    @Test
    public void testAddTaoComponent() {
        ResponseEntity<String> response = addTaoComponent(null, null);
        checkReceivedComponent(response.getBody());
    }

    @Test
    public void deleteTaoComponent() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        String fullUrl = createURLWithPort("/component/segmentation-cc-1");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.DELETE, entity, String.class);
        System.out.println(response);

        checkTaoComponentExistence();
    }

    private ResponseEntity<String> addTaoComponent(String id, String label) {
        String newCompJson = testComponentJson;
        if (id != null) {
            newCompJson = newCompJson.replaceFirst("    \"id\": \"segmentation-cc-1\",", "    \"id\": \"" + id + "\",");
        }
        if (label != null) {
            newCompJson = newCompJson.replaceFirst("    \"label\": \"First segmentation component\",", "    \"label\": \"" + label + "\",");
        }

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

    private void checkTaoComponentExistence() {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/segmentation-cc-1");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkReceivedComponent(response.getBody());
    }

    private void checkReceivedComponent(String responseBody) {
        JsonFactory factory = new JsonFactory();
        JsonParser parser = null;
        try {
            parser = factory.createParser(responseBody);
            String idVal = null;
            String labelVal = null;
            String containerIdVal = null;
            while(!parser.isClosed()){
                JsonToken jsonToken = parser.nextToken();
                if(JsonToken.FIELD_NAME.equals(jsonToken)){
                    String fieldName = parser.getCurrentName();
                    System.out.println(fieldName);
                    if("id".equals(fieldName) && (idVal == null)){
                        jsonToken = parser.nextToken();
                        idVal = parser.getValueAsString();
                    } else if ("label".equals(fieldName) && (labelVal == null)){
                        jsonToken = parser.nextToken();
                        labelVal = parser.getValueAsString();
                    } else if ("containerId".equals(fieldName) && (containerIdVal == null)){
                        jsonToken = parser.nextToken();
                        containerIdVal = parser.getValueAsString();
                    }
                }
            }
            if (!"segmentation-cc-1".equals(idVal) || !"First segmentation component".equals(labelVal) ||
                    !"DummyTestDockerContainer".equals(containerIdVal)) {
                Assert.assertFalse(true);
            }
        } catch (IOException e){
            Assert.assertFalse(true);
        }
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
            "    \"active\": false," +
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