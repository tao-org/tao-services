package ro.cs.tao.wps.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.services.model.workflow.WorkflowInfo;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class WPSControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getCapabilities() throws Exception {
        //execution
        final ResponseEntity<List<WorkflowInfo>> mvcResult = restTemplate.getForObject("/wps/getCapabilities", ResponseEntity.class);

        //verification
        assertThat(mvcResult).isNotNull();
    }

    @Test
    public void describeProcess() {
    }

    @Test
    public void execute() {
    }
}