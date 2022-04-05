package ro.cs.tao.wps.impl;

import com.bc.wps.WpsRequestContextImpl;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.utilities.JaxbHelper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BCWpsServiceInstanceImpl_GetStatusTest {

    WebProcessingService taoWebProcessingServiceMock;
    BCWpsServiceInstanceImpl bcWpsServiceInstanceImpl;
    WpsRequestContextImpl requestContext;

    @Before
    public void setUp() throws Exception {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8092/wps"));
        requestContext = new WpsRequestContextImpl(servletRequest);

        taoWebProcessingServiceMock = Mockito.mock(WebProcessingService.class);

        bcWpsServiceInstanceImpl = new BCWpsServiceInstanceImpl();
        bcWpsServiceInstanceImpl.setTaoWpsImpl(taoWebProcessingServiceMock);
    }

    @Test
    public void getCapabilities() {
    }

    @Test
    public void describeProcess() {
    }

    @Test
    public void testDoExecute_responseFormNotGiven() throws WpsServiceException {

        final LocalDateTime nowLocal = LocalDateTime.now();
        System.out.println("nowLocal = " + nowLocal);
        final ZonedDateTime nowZoned = ZonedDateTime.now();
        System.out.println("nowZoned = " + nowZoned);
        final ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);
        System.out.println("nowUTC = " + nowUTC);
        final GregorianCalendar gNowZoned = GregorianCalendar.from(nowZoned);
        System.out.println("gNowZoned = " + gNowZoned.getTime());
        final GregorianCalendar gNowUTC = GregorianCalendar.from(nowUTC);
        System.out.println("gNowUTC = " + gNowUTC.getTime());
    }

    @Test
    public void testGetStatus_Undetermined() throws JAXBException, WpsServiceException {
        //preparation
        final ExecutionJob executionJob = new ExecutionJob();
        executionJob.setExecutionStatus(ExecutionStatus.UNDETERMINED);
        executionJob.setWorkflowId(456L);
        when(taoWebProcessingServiceMock.getStatus(123)).thenReturn(executionJob);
        final WebProcessingServiceImpl.ProcessInfoImpl processInfo = new WebProcessingServiceImpl.ProcessInfoImpl();
        final WorkflowDescriptor workflowDescriptor = new WorkflowDescriptor();

        final WorkflowInfo workflowInfo = new WorkflowInfo(workflowDescriptor);

        processInfo.setWorkflowInfo(workflowInfo);
        when(taoWebProcessingServiceMock.describeProcess(456)).thenReturn(processInfo);

        //execution
        final ExecuteResponse status = bcWpsServiceInstanceImpl.getStatus(requestContext, "123");

        //verification
        final String marshal = JaxbHelper.marshal(status);
        final String[] expected = {
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
                "<wps:ExecuteResponse serviceInstance=\"http://localhost:8092/wps\" statusLocation=\"http://localhost:8092/wps?Service=WPS&amp;Request=GetStatus&amp;JobId=123\" service=\"WPS\" version=\"1.0.0\" xml:lang=\"en\" xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">",
                "    <wps:Process>".trim(),
                "        <ows:Identifier>0</ows:Identifier>".trim(),
                "        <ows:Title/>".trim(),
                "    </wps:Process>".trim(),
//                "<wps:Status creationTime=\"2018-11-13T17:29:44.218+01:00\">",
                "    <wps:Status creationTime=\"2".trim(),
                "-",
                "-",
                "T",
                ":",
                ":",
                ".",
                "+",
                ":",
                "\">",
                "        <wps:ProcessAccepted>TAO status Not started</wps:ProcessAccepted>".trim(),
                "    </wps:Status>".trim(),
                "</wps:ExecuteResponse>"
        };
        assertThat(marshal, Matchers.stringContainsInOrder(Arrays.asList(expected)));
    }

    @Test
    public void dispose() {
    }

    @Test
    public void getProcessOutputs() {
    }

    @Test
    public void getDataInputs() {
    }
}