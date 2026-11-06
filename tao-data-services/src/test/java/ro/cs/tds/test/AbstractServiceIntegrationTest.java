package ro.cs.tds.test;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Array;

/**
 * Created by cosmin on 11/24/2017.
 */
public abstract class AbstractServiceIntegrationTest <T> {

    @LocalServerPort
    private int port;
    private RestTemplate restTemplate = new RestTemplate();

    private Class<T> classItem;
    protected abstract String getUrlMappingStr();
    protected abstract String getTestItemId();
    protected abstract String getTestItemJson();
    protected abstract boolean checkItem(T item, boolean isActive);

    protected AbstractServiceIntegrationTest(Class<T> classItem) {
        this.classItem = classItem;
    }

    @Test
    public void testGetTaoItems() {
        // add the item if does not exists already
        addTaoItem();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort(getUrlMappingStr());
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkGetItemResponseBody(response.getBody(), true, false);
    }

    @Test
    public void testGetSingleTaoItem() {
        // add the item if does not exists already
        addTaoItem();

        checkTaoItemExistence(false);
    }

    @Test
    public void testAddTaoItem() {
        ResponseEntity<String> response = addTaoItem();
        if (response == null) {
            Assert.assertFalse(true);
        }
        checkGetItemResponseBody(response.getBody(), false, false);
    }

    @Test
    public void deleteTaoItem() {
        // First add (or update) an active item
        addTaoItem();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        String fullUrl = createURLWithPort(getUrlMappingStr() + getTestItemId());
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.DELETE, entity, String.class);
        System.out.println(response);

        checkTaoItemExistence(true);
    }

    protected ResponseEntity<String> addTaoItem() {
        String newCompJson = getTestItemJson();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        entity = new HttpEntity<>(newCompJson, headers);
        String fullUrl = createURLWithPort(getUrlMappingStr());
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.POST, entity, String.class, newCompJson);
        System.out.println(response);
        return response;
    }

    private void checkTaoItemExistence(boolean checkIfDeleted) {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        String fullUrl = createURLWithPort(getUrlMappingStr() + getTestItemId());
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET, entity, String.class);

        System.out.println(fullUrl);
        System.out.println(response);
        System.out.println(response.getBody());
        checkGetItemResponseBody(response.getBody(), false, checkIfDeleted);
    }

    private void checkGetItemResponseBody(String responseBody, boolean isArray, boolean checkIfDeleted) {
        if (checkIfDeleted && responseBody.matches(".*Entity.*not found.*$")) {
            // check if the item was really deleted and does not exist in the database
            return;
        }
        ObjectMapper objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        T[] items = null;
        if (isArray) {
            Class<? extends T[]> arrayType =
                    (Class<? extends T[]>) Array.newInstance(classItem, 0).getClass();
            items = objectMapper.readValue(responseBody, arrayType);
        } else {
            items = (T[])Array.newInstance(classItem, 1);
            items[0] = objectMapper.readValue(responseBody, classItem);
        }
        for (int i = 0; i<items.length; i++) {
            T item = (T)items[i];
            boolean isActive = !checkIfDeleted;
            if (checkItem(item, isActive)) {
                return;
            }
        }
        Assert.assertFalse(true);

    }
    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }
}
