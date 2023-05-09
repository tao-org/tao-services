package ro.cs.tao.wps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.DataDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.enums.Status;
import ro.cs.tao.wps.controllers.WPSController;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@WebMvcTest(WPSController.class)
public class WPSIntegrationTest_SpringMocking {

    Object settingsBeforeTest;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private WorkflowService workflowService;
    @MockBean
    private OrchestratorService orchestratorService;
    @MockBean
    private ExecutionJobProvider jobProvider;
    @MockBean
    private StorageService<MultipartFile, FileSystemResource> storageService;

    @Before
    public void setUp() throws Exception {
        prepareConfigurationManagerForTest();
    }

    @After
    public void tearDown() throws Exception {
        resetConfigurationManager();
    }

    @Test
    @WithMockUser()
    public void testGetCapabilities_WithTwoProcessOfferings() throws Exception {
        //preparation
        given(this.workflowService.getPublicWorkflows())
                .willReturn(Arrays.asList(createWorkflowInfo("WF1", 4L), createWorkflowInfo("WF2", 5L)));

        //execution
        final long before = System.currentTimeMillis();
//        final long before = new Date().getTime();
        final ResultActions result = mvc.perform(
                get("/wps").param("Service", "WPS")
                        .param("Request", "GetCapabilities")
        );
        final long after = System.currentTimeMillis();
//        final long after = new Date().getTime();

        //verification
        result.andExpect(status().isOk());
        result.andExpect(header().exists("Date"));
        result.andExpect(header().string(HttpHeaders.TRANSFER_ENCODING, "chunked"));
        result.andExpect(content().contentType(MediaType.APPLICATION_XML));
        result.andExpect(content().string(is(equalToIgnoringWhiteSpace(getResourceAsString("Expected_Capabilities_1.xml")))));
    }

    @Test
    @WithMockUser()
    public void testDescribeProcess_withThreeInputParameter() throws Exception {
        //preparation
        Map<String, List<Parameter>> givenMap = new HashMap<>();
        final Parameter parameterA = new Parameter("PNameA", Integer.class.getName(), null);
        final Parameter parameterB = new Parameter("PNameB", String.class.getName(), null, new String[]{"c", "d", "e"});
        final Parameter parameterC = new Parameter("PNameC", Integer.class.getName(), "4");
        givenMap.put("PGroup", Arrays.asList(parameterA, parameterB, parameterC));
        given(this.orchestratorService.getWorkflowParameters(24L))
                .willReturn(givenMap);

        given(this.workflowService.getWorkflowInfo(24))
                .willReturn(createWorkflowInfo("WorkflowName", 24));

        final DataDescriptor dataDescriptor = new DataDescriptor();
        dataDescriptor.setLocation("dataLocation");
        final TargetDescriptor output = new TargetDescriptor("9108h1f0nqndq");
        output.setDataDescriptor(dataDescriptor);
        given(orchestratorService.getWorkflowOutputs(24))
                .willReturn(Arrays.asList(new TargetDescriptor[]{output}));

        //execution
        final ResultActions result = mvc.perform(
                get("/wps").param("Service", "WPS")
                        .param("Request", "DescribeProcess")
                        .param("Identifier", "24")
                        .param("Version", "1.0.0")
        );

        //verification
        result.andExpect(status().isOk());
        result.andExpect(header().exists("Date"));
        result.andExpect(header().string(HttpHeaders.TRANSFER_ENCODING, "chunked"));
        result.andExpect(content().contentType(MediaType.APPLICATION_XML));
        result.andExpect(content().string(is(equalToIgnoringWhiteSpace(getResourceAsString("Expected_DescribeProcessResponse_1.xml")))));
    }

    @Test
    @WithMockUser()
    public void testGetStatus() throws Exception {
        //preparation
        final ExecutionJob job = new ExecutionJob();
        job.setWorkflowId(24L);
        job.setExecutionStatus(ExecutionStatus.RUNNING);

        given(this.jobProvider.get(13L)).willReturn(job);
        given(this.workflowService.getWorkflowInfo(24)).willReturn(createWorkflowInfo("TestWorkflowName", 24));

        //execution
        final ResultActions result = mvc.perform(
                get("/wps").param("Service", "WPS")
                        .param("Request", "GetStatus")
                        .param("JobId", "13"));

        //verification
        result.andExpect(status().isOk());
        result.andExpect(header().exists("Date"));
        result.andExpect(header().string(HttpHeaders.TRANSFER_ENCODING, "chunked"));
        result.andExpect(content().contentType(MediaType.APPLICATION_XML));
        result.andExpect(content().string(stringContainsInOrder(expectedStatusParts())));
    }

    @Test
    @WithMockUser()
    public void testRequestGet_invalidService_serviceIsNotWPS() throws Exception {
        //preparation
        final String service = "AAAAAAA"; // valid value = "WPS"

        //execution
        final ResultActions result = mvc.perform(
                get("/wps").param("Service", service)
                        .param("Request", "GetStatus")
                        .param("JobId", "13"));

        //verification
        result.andExpect(status().isOk());
        result.andExpect(header().exists("Date"));
        result.andExpect(header().string(HttpHeaders.TRANSFER_ENCODING, "chunked"));
        result.andExpect(content().contentType(MediaType.APPLICATION_XML));
        result.andExpect(content().string(is(equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                                                     "<ows:ExceptionReport version=\"1.0.0\" xml:lang=\"en\" xsi:schemaLocation=\"http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd\"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                                                     "    <ows:Exception exceptionCode=\"NoApplicableCode\">\n" +
                                                     "        <ows:ExceptionText>No such Service: http://localhost/wps?Service=AAAAAAA&amp;Request=GetStatus&amp;JobId=13</ows:ExceptionText>\n" +
                                                     "    </ows:Exception>\n" +
                                                     "</ows:ExceptionReport>\n"))));
    }

    @Test
    @WithMockUser()
    public void testRequestGet_invalidRequest_requestIsNotValid() throws Exception {
        //preparation
        final String requestType = "BBBBBBBBBBBBB";  // valid values = "GetCapabilities" or "DescribeProcess"

        //execution
        //execution
        final ResultActions result = mvc.perform(
                get("/wps").param("Service", "WPS")
                        .param("Request", requestType)
                        .param("JobId", "13"));

        //verification
        result.andExpect(status().isOk());
        result.andExpect(header().exists("Date"));
        result.andExpect(header().string(HttpHeaders.TRANSFER_ENCODING, "chunked"));
        result.andExpect(content().contentType(MediaType.APPLICATION_XML));
        result.andExpect(content().string(is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ows:ExceptionReport version=\"1.0.0\" xml:lang=\"en\" xsi:schemaLocation=\"http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd\"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                "    <ows:Exception exceptionCode=\"NoApplicableCode\">\n" +
                "        <ows:ExceptionText>No such Service: http://localhost/wps?Service=WPS&amp;Request=BBBBBBBBBBBBB&amp;JobId=13</ows:ExceptionText>\n" +
                "    </ows:Exception>\n" +
                "</ows:ExceptionReport>\n"))));
    }

    private WorkflowInfo createWorkflowInfo(String workflowName, long id) {
        final WorkflowDescriptor workflow = new WorkflowDescriptor();
        workflow.setName(workflowName);
        workflow.setId(id);
        workflow.setStatus(Status.READY);
        workflow.setVisibility(Visibility.PUBLIC);
        return new WorkflowInfo(workflow);
    }

    private void prepareConfigurationManagerForTest() {
        final ConfigurationProvider configurationManager = ConfigurationManager.getInstance();
        settingsBeforeTest = ReflectionTestUtils.getField(configurationManager, "settings");
        final Properties properties = new Properties();
        properties.setProperty("company.name", "Test.company.name");
        properties.setProperty("company.website", "Test.company.website");
        properties.setProperty("project.manager.name", "Test.roject.manager.name");
        properties.setProperty("project.manager.position.name", "Test.project.manager.position.name");
        properties.setProperty("company.phone.number", "Test.company.phone.number");
        properties.setProperty("company.fax.number", "Test.company.fax.number");
        properties.setProperty("company.address", "Test.company.address");
        properties.setProperty("company.city", "Test.company.city");
        properties.setProperty("company.administrative.area", "Test.company.administrative.area");
        properties.setProperty("company.post.code", "Test.company.post.code");
        properties.setProperty("company.country", "Test.company.country");
        properties.setProperty("company.email.address", "Test.company.email.address");
        properties.setProperty("company.service.hours", "Test.company.service.hours");
        properties.setProperty("company.contact.instruction", "Test.company.contact.instruction");
        properties.setProperty("wps.service.id", "Test.wps.service.id");
        properties.setProperty("wps.service.abstract", "Test.wps.service.abstract");
        properties.setProperty("wps.service.type", "Test.wps.service.type");
        properties.setProperty("wps.version", "Test.wps.version");
        properties.setProperty("wps.default.lang", "Test.wps.default.lang");
        properties.setProperty("wps.supported.lang", "Test.wps.supported.lang");
        ReflectionTestUtils.setField(configurationManager, "settings", properties);
    }

    private void resetConfigurationManager() {
        final ConfigurationProvider configurationManager = ConfigurationManager.getInstance();
        ReflectionTestUtils.setField(configurationManager, "settings", settingsBeforeTest);
    }

    private String getResourceAsString(String name) throws IOException {
        final InputStream resource = getClass().getResourceAsStream(name);
        final StringBuilder stringBuilder = new StringBuilder();
        final byte[] bytes = new byte[8000];
        while (resource.read(bytes) > 0) {
            stringBuilder.append(new String(bytes).trim());
        }
        return stringBuilder.toString();
    }

    private List<String> expectedStatusParts() {
        return Arrays.asList(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
                "<wps:ExecuteResponse serviceInstance=\"http://localhost/wps\"",
                /**/                " statusLocation=\"http://localhost/wps?Service=WPS&amp;Request=GetStatus&amp;JobId=13\"",
                /**/                " service=\"WPS\"",
                /**/                " version=\"1.0.0\"",
                /**/                " xml:lang=\"en\"",
                /**/                " xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd\"",
                /**/                " xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"",
                /**/                " xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"",
                /**/                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">",
                /**/"<wps:Process>",
                /**/    "<ows:Identifier>24</ows:Identifier>",
                /**/    "<ows:Title>TestWorkflowName</ows:Title>",
                /**/"</wps:Process>",
                /**/"<wps:Status creationTime=\"", "-", "-", "T", ":", ":", ".", "\">",
                /**/    "<wps:ProcessStarted>TAO status Running</wps:ProcessStarted>",
                /**/"</wps:Status>",
                "</wps:ExecuteResponse>");
    }
}