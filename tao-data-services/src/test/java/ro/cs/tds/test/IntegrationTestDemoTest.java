package ro.cs.tds.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.services.app.ComponentApplication;

/**
 * Created by cosmin on 11/24/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComponentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTestDemoTest {

    @LocalServerPort
    private int port;

    TestRestTemplate restTemplate = new TestRestTemplate();


    @Test
    public void testGetTaoComponents() {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        //JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    public void testGetSingleTaoComponent() {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/segmentation-cc-1");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        //JSONAssert.assertEquals(expected, response.getBody(), false);
    }


    @Test
    public void testAddTaoComponent() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort("/component/");
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        String newItemStr = response.getBody();

        headers.setContentType(MediaType.APPLICATION_JSON);
        newItemStr = newItemStr.replaceFirst("\\{", "{\"@type\":\"ProcessingComponent\", ");
        newItemStr = newItemStr.replaceFirst("\\[", "");
        newItemStr = newItemStr.substring(0, newItemStr.length()-1);
        newItemStr = newItemStr.substring(0, newItemStr.indexOf(",{\"id\":\"segmentation-cc-2\","));

        entity = new HttpEntity<String>(newItemStr, headers);
        String fullUrl2 = createURLWithPort("/component/");
        ResponseEntity<String> response2 = restTemplate.exchange(
                fullUrl2,
                HttpMethod.POST, entity, String.class, newItemStr);
        System.out.println(response2);
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }


}