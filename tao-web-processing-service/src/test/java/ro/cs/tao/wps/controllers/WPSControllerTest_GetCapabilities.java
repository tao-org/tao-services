package ro.cs.tao.wps.controllers;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.OptionNotSupportedException;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.Capabilities;
import org.junit.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.wps.impl.BCWpsServiceInstanceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

public class WPSControllerTest_GetCapabilities {

    private final String validService = "WPS";
    private final String validRequestType_GC = "GetCapabilities";
    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private StringWriter responseBody;
    private WPSController wpsController;
    private WpsServiceInstance mockBcWpsServiceInstance;
    private WebProcessingService mockTaoWebProcessingService;

    @Before
    public void setUp() throws Exception {
        httpRequest = mock(HttpServletRequest.class);
        httpResponse = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(httpResponse.getWriter()).thenReturn(new PrintWriter(responseBody));

        mockBcWpsServiceInstance = mock(WpsServiceInstance.class);
        mockTaoWebProcessingService = mock(WebProcessingService.class);

        // class under test
        wpsController = new WPSController();
    }

    @Test
    public void testRequestGet_ValidGetCapabilitiesRequest() throws Exception {
        //preparation
        replaceBcWpsServiceInstanceWithMock();
        final Capabilities validCapabilities = new Capabilities();
        when(mockBcWpsServiceInstance.getCapabilities(any(WpsRequestContext.class))).thenReturn(validCapabilities);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.capabilities(httpRequest, httpResponse);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(responseBody.toString(), is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<wps:Capabilities xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsGetCapabilities_response.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n")));
    }

    @Test
    public void testRequestGet_WpsServiceException() throws Exception {
        //preparation
        replaceBcWpsServiceInstanceWithMock();
        WpsServiceException e = new OptionNotSupportedException("Message", "Identifier");
        when(mockBcWpsServiceInstance.getCapabilities(any(WpsRequestContext.class))).thenThrow(e);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.capabilities(httpRequest, httpResponse);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(responseBody.toString(), is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ows:ExceptionReport version=\"1.0.0\" xml:lang=\"en\" xsi:schemaLocation=\"http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                "    <ows:Exception exceptionCode=\"OptionNotSupported\" locator=\"Identifier\">\n" +
                "        <ows:ExceptionText>Message</ows:ExceptionText>\n" +
                "    </ows:Exception>\n" +
                "</ows:ExceptionReport>\n")));
    }

    private void replaceTaoWebProcessingServiceInBcWpsServiceWithMock() {
        final BCWpsServiceInstanceImpl wpsServiceInstance = (BCWpsServiceInstanceImpl) ReflectionTestUtils.getField(wpsController, "wpsServiceInstance");
        ReflectionTestUtils.setField(wpsServiceInstance, "webProcessingService", mockTaoWebProcessingService);
    }

    private void replaceBcWpsServiceInstanceWithMock() {
        ReflectionTestUtils.setField(wpsController, "wpsServiceInstance", mockBcWpsServiceInstance);
    }
}