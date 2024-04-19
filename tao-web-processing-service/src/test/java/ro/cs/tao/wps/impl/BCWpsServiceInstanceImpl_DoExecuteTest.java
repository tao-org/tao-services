package ro.cs.tao.wps.impl;

import com.bc.wps.WpsRequestContextImpl;
import com.bc.wps.api.exceptions.OptionNotSupportedException;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.*;
import org.junit.Before;
import org.junit.Test;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BCWpsServiceInstanceImpl_DoExecuteTest {

    WebProcessingService taoWebProcessingServiceMock;
    BCWpsServiceInstanceImpl bcWpsServiceInstanceImpl;
    WpsRequestContextImpl requestContext;
    Execute executeRequest;

    @Before
    public void setUp() throws Exception {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8092/wps"));
        requestContext = new WpsRequestContextImpl(servletRequest);

        taoWebProcessingServiceMock = mock(WebProcessingService.class);

        executeRequest = createValidExecuteSchema();

        bcWpsServiceInstanceImpl = new BCWpsServiceInstanceImpl();
        bcWpsServiceInstanceImpl.setTaoWpsImpl(taoWebProcessingServiceMock);
    }

    @Test
    public void testDoExecute_responseFormNotGiven() {
        //preparation
        executeRequest.setResponseForm(null);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "Execute needs a ResponseForm element",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("ResponseForm missed in element Execute")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_responseDocumentNotGiven() {
        //preparation
        executeRequest.getResponseForm().setResponseDocument(null);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "ResponseForm element needs a ResponseDocument element",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("ResponseDocument missed in element ResponseForm")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_storeExecuteResponse_NotGiven() {
        //preparation
        executeRequest.getResponseForm().getResponseDocument().setStoreExecuteResponse(null);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "ResponseDocument needs attribute storeExecuteResponse=\"true\"",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("storeExecuteResponse=\"false\" or attribute is missed")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_storeExecuteResponse_false() {
        //preparation
        executeRequest.getResponseForm().getResponseDocument().setStoreExecuteResponse(false);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "ResponseDocument needs attribute storeExecuteResponse=\"true\"",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("storeExecuteResponse=\"false\" or attribute is missed")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_statusAttribute_notGiven() {
        //preparation
        executeRequest.getResponseForm().getResponseDocument().setStatus(null);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "ResponseDocument needs attribute status=\"true\"",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("status=\"false\" or attribute is missed")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_statusAttribute_false() {
        //preparation
        executeRequest.getResponseForm().getResponseDocument().setStatus(false);

        try {
            //execution
            bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

            //verification
            fail("OptionNotSupportedException expected");
        } catch (OptionNotSupportedException expected) {
            assertThat(expected.getMessage(), stringContainsInOrder(Arrays.asList(
                    "ResponseDocument needs attribute status=\"true\"",
                    " because TAO WPS service allows only asynchronous execution.")));
            assertThat(expected.getIdentifierOfOptionNotSupported(), is(equalTo("status=\"false\" or attribute is missed")));
        } catch (Exception notExpected) {
            fail(notExpected.getClass().getSimpleName() + " not expected");
        }
    }

    @Test
    public void testDoExecute_valid() throws WpsServiceException {
        //preparation
        when(taoWebProcessingServiceMock.execute(eq(1234L), anyMap())).thenReturn(567L);
        final ExecutionJob executionJob = new ExecutionJob();
        executionJob.setExecutionStatus(ExecutionStatus.UNDETERMINED);
        executionJob.setWorkflowId(1234L);
        when(taoWebProcessingServiceMock.getStatus(567L)).thenReturn(executionJob);
        final WebProcessingServiceImpl.ProcessInfoImpl processInfo = new WebProcessingServiceImpl.ProcessInfoImpl();
        final WorkflowDescriptor workflowDescriptor = new WorkflowDescriptor();
        workflowDescriptor.setId(1234L);
        workflowDescriptor.setName("WorkflowName");
        processInfo.setWorkflowInfo(new WorkflowInfo(workflowDescriptor, null));
        when(taoWebProcessingServiceMock.describeProcess(1234L)).thenReturn(processInfo);

        //execution
        final ExecuteResponse executeResponse = bcWpsServiceInstanceImpl.doExecute(requestContext, executeRequest);

        //verification
        assertNotNull(executeResponse);
        assertEquals("WPS", executeResponse.getService());
        assertEquals("1.0.0", executeResponse.getVersion());
        assertEquals("en", executeResponse.getLang());
        assertNotNull(executeResponse.getStatus());
        final XMLGregorianCalendar creationTime = executeResponse.getStatus().getCreationTime();
        assertNotNull(creationTime);
        final ZonedDateTime zdt = creationTime.toGregorianCalendar().toZonedDateTime();
        assertThat(ZonedDateTime.now().toEpochSecond() - zdt.toEpochSecond(), is(lessThan(10L)) );
        assertEquals("TAO status Not started", executeResponse.getStatus().getProcessAccepted());
        assertEquals("1234",executeResponse.getProcess().getIdentifier().getValue());
        assertEquals("WorkflowName",executeResponse.getProcess().getTitle().getValue());
        assertEquals("http://localhost:8092/wps", executeResponse.getServiceInstance());
        assertEquals("http://localhost:8092/wps?Service=WPS&Request=GetStatus&JobId=567", executeResponse.getStatusLocation());
    }

    private Execute createValidExecuteSchema() {
        final ResponseDocumentType responseDocument = new ResponseDocumentType();
        responseDocument.setStoreExecuteResponse(true);
        responseDocument.setStatus(true);

        final ResponseFormType responseForm = new ResponseFormType();
        responseForm.setResponseDocument(responseDocument);

        Execute execute = new Execute();
        execute.setResponseForm(responseForm);
        final CodeType identifier = new CodeType();
        identifier.setValue("1234");
        execute.setIdentifier(identifier);
        return execute;
    }
}