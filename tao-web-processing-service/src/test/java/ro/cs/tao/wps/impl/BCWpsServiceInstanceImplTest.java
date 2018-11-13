package ro.cs.tao.wps.impl;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.utilities.JaxbHelper;
import org.junit.*;
import org.mockito.Mockito;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.services.interfaces.WebProcessingService;

import javax.xml.bind.JAXBException;

public class BCWpsServiceInstanceImplTest {

    WebProcessingService wpsMock;
    BCWpsServiceInstanceImpl testable;

    @Before
    public void setUp() throws Exception {
        wpsMock = Mockito.mock(WebProcessingService.class);

        testable = new BCWpsServiceInstanceImpl();
        testable.setTaoWpsImpl(wpsMock);
    }

    @Test
    public void getCapabilities() {
    }

    @Test
    public void describeProcess() {
    }

    @Test
    public void doExecute() {
    }

    @Test
    public void getStatus_Undetermined() throws JAXBException, WpsServiceException {
        //preparation
        final ExecutionJob executionJob = new ExecutionJob();
        executionJob.setExecutionStatus(ExecutionStatus.UNDETERMINED);
        Mockito.when(wpsMock.getStatus(123)).thenReturn(executionJob);

        //execution
        final ExecuteResponse status = testable.getStatus(null, "123");

        //verification
        final String marshal = JaxbHelper.marshal(status);
        assertEquals(marshal, "");
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