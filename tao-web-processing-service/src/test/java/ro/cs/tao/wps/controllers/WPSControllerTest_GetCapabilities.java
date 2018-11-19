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

import javax.servlet.http.HttpServletRequest;

public class WPSControllerTest_GetCapabilities {

    private final String validService = "WPS";
    private final String validRequestType_GC = "GetCapabilities";

    private HttpServletRequest httpRequest;
    private WPSController wpsController;
    WpsServiceInstance wpsServiceInstance;

    @Before
    public void setUp() throws Exception {
        httpRequest = mock(HttpServletRequest.class);

        wpsServiceInstance = mock(WpsServiceInstance.class);

        // class under test
        wpsController = new WPSController();
        ReflectionTestUtils.setField(wpsController, "wpsServiceInstance", wpsServiceInstance);
    }

    @Test
    public void testRequestGet_ValidGetCapabilitiesRequest() throws Exception {
        //preparation
        final Capabilities validCapabilities = new Capabilities();
        when(wpsServiceInstance.getCapabilities(any(WpsRequestContext.class))).thenReturn(validCapabilities);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestGet(validService, validRequestType_GC,null, null, null, null, null, httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(responseEntity.getBody(), is(equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                                                       "<wps:Capabilities xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsGetCapabilities_response.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n")));
    }

    @Test
    public void testRequestGet_invalidService_serviceIsNotWPS() throws Exception {
        //preparation
        final String service = "AAAAAAA"; // valid value = "WPS"

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestGet(service, validRequestType_GC, null, null, null, null, null, httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.SERVICE_UNAVAILABLE)));
        assertThat(responseEntity.getBody(), is(instanceOf(String.class)));
        assertThat((String)responseEntity.getBody(), containsString(service));
    }

    @Test
    public void testRequestGet_invalidRequest_requestIsNotValid() throws Exception {
        //preparation
        final String requestType = "BBBBBBBBBBBBB";  // valid values = "GetCapabilities" or "DescribeProcess"

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestGet(validService, requestType, null, null, null, null, null, httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.SERVICE_UNAVAILABLE)));
        assertThat(responseEntity.getBody(), is(instanceOf(String.class)));
        assertThat((String)responseEntity.getBody(), containsString(requestType));
    }

    @Test
    public void testRequestGet_WpsServiceException() throws Exception {
        //preparation
        WpsServiceException e = new OptionNotSupportedException("Message", "Identifier");
        when(wpsServiceInstance.getCapabilities(any(WpsRequestContext.class))).thenThrow(e);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestGet(validService, validRequestType_GC, null, null, null, null, null, httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(responseEntity.getBody(), equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                                                     "<ows:ExceptionReport version=\"1.0.0\" xml:lang=\"en\" xsi:schemaLocation=\"http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                                                     "    <ows:Exception exceptionCode=\"OptionNotSupported\" locator=\"Identifier\">\n" +
                                                     "        <ows:ExceptionText>Message</ows:ExceptionText>\n" +
                                                     "    </ows:Exception>\n" +
                                                     "</ows:ExceptionReport>\n"));
    }
}