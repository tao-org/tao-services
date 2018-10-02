package ro.cs.tao.wps.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.bc.wps.WpsFrontendConnector;
import org.junit.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class WPSControllerTest {

    final String validService = "WPS";
    final String validRequestType_GC = "GetCapabilities";

    WpsFrontendConnector frontendConnector;
    HttpServletRequest httpRequest;
    HttpServletResponse servletResponse;
    StringWriter responseWriter;
    WPSController classUnderTest;

    @Before
    public void setUp() throws Exception {
        httpRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);
        frontendConnector = mock(WpsFrontendConnector.class);
        responseWriter = new StringWriter();
        when(servletResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // preparation for valid GetCapabilities request
        when(frontendConnector.getWpsService("TAO", "WPS", "GetCapabilities",
                                             null, null, null, null, null,
                                             httpRequest)).thenReturn("ValidResponse");

        classUnderTest = new WPSController();
        ReflectionTestUtils.setField(classUnderTest, "wpsFrontendConnector", frontendConnector);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(frontendConnector);
        verifyNoMoreInteractions(servletResponse);
        verifyNoMoreInteractions(httpRequest);
    }

    @Test
    public void testRequestGet_ValidGetCapabilitiesRequest() throws Exception {
        //preparation
        when(frontendConnector.getWpsService(anyString(), anyString(), anyString(),
                                             isNull(), isNull(), isNull(), isNull(), isNull(),
                                             same(httpRequest))).thenReturn("Valid frontendConnector Response");

        //execution
        final ResponseEntity<?> responseEntity = classUnderTest
                .requestGet(validService, validRequestType_GC, null, null, null, null, null, httpRequest, servletResponse);

        //verification
        assertThat(responseEntity).isEqualTo(ResponseEntity.accepted().build());
        assertThat(responseWriter.toString()).isEqualTo("Valid frontendConnector Response");
        validGetCapabilitiesCallToFrontendConnectorExpected();
        verify(servletResponse, times(1)).getWriter();
        verify(servletResponse, times(1)).setContentType(MediaType.APPLICATION_XML_VALUE);
        verify(servletResponse, times(1)).setDateHeader(eq("Date"), anyLong());
        verify(servletResponse, times(1)).setHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
    }

    @Test
    public void testRequestGet_invalidService_serviceIsNotWPS() throws Exception {
        //preparation
        final String service = "AAAAAAA"; // valid value = "WPS"

        //execution
        final ResponseEntity<?> responseEntity = classUnderTest.requestGet(service, validRequestType_GC, null, null, null, null, null, httpRequest, servletResponse);

        //verification
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(responseEntity.getBody()).isInstanceOf(String.class);
        assertThat(responseEntity.getBody()).matches(o -> ((String) o).contains(service));
    }

    @Test
    public void testRequestGet_invalidRequest_requestIsNotValid() throws Exception {
        //preparation
        final String requestType = "BBBBBBBBBBBBB";  // valid values = "GetCapabilities" or "DescribeProcess"

        //execution
        final ResponseEntity<?> responseEntity = classUnderTest.requestGet(validService, requestType, null, null, null, null, null, httpRequest, servletResponse);

        //verification
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(responseEntity.getBody()).isInstanceOf(String.class);
        assertThat(responseEntity.getBody()).matches(o -> ((String) o).contains(requestType));
    }

    @Test
    public void testRequestGet_IOExceptionWhileWritingToResponceWriter() throws Exception {
        //preparation
        when(servletResponse.getWriter()).thenThrow(new IOException("TestException"));

        //execution
        final ResponseEntity<?> responseEntity = classUnderTest
                .requestGet(validService, validRequestType_GC, null, null, null, null, null, httpRequest, servletResponse);

        //verification
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(responseEntity.getBody()).isEqualTo("IO Exception while writing response.");
        validGetCapabilitiesCallToFrontendConnectorExpected();
        verify(servletResponse, times(1)).getWriter();
    }

    private void validGetCapabilitiesCallToFrontendConnectorExpected() {
        verify(frontendConnector, times(1)).getWpsService("TAO", validService, validRequestType_GC,
                                                          null, null, null, null, null,
                                                          httpRequest);
    }
}