package ro.cs.tao.wps.controllers;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.StatusType;
import org.junit.*;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Locale;

public class WPSControllerTest_Execute {

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private StringWriter responseBody;
    private WPSController wpsController;
    private WpsServiceInstance wpsServiceInstance;

    @Before
    public void setUp() throws Exception {
        httpRequest = mock(HttpServletRequest.class);
        httpResponse = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(httpResponse.getWriter()).thenReturn(new PrintWriter(responseBody));

        wpsServiceInstance = mock(WpsServiceInstance.class);

        // class under test
        wpsController = new WPSController();
        ReflectionTestUtils.setField(wpsController, "wpsServiceInstance", wpsServiceInstance);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(httpResponse);
    }

    @Test
    public void testExecute_ValidRequest() throws Exception {
        //preparation
        when(httpRequest.getReader()).thenReturn(new BufferedReader(new StringReader(validExecute())));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest, httpResponse);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        verify(httpResponse, times(1)).setHeader("Transfer-Encoding", "chunked");
        verify(httpResponse, times(1)).setDateHeader(eq("Date"), Mockito.anyLong());
        verify(httpResponse, times(1)).setContentType("application/xml");
        verify(httpResponse, times(1)).getWriter();
        assertThat(responseBody.toString(), is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> " +
                "<wps:ExecuteResponse xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                "    <wps:Status>" +
                "        <wps:ProcessAccepted>AAAAAAAAAAAA</wps:ProcessAccepted>" +
                "    </wps:Status> " +
                "</wps:ExecuteResponse>")));
    }

    @Test
    public void testExecute_NotAnExecuteRequest() throws Exception {
        //preparation
        when(httpRequest.getReader()).thenReturn(new BufferedReader(new StringReader( "<any><other>xml</other></any>   ")));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest, httpResponse);

        //verification
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        verify(httpResponse, times(1)).setHeader("Transfer-Encoding", "chunked");
        verify(httpResponse, times(1)).setDateHeader(eq("Date"), Mockito.anyLong());
        verify(httpResponse, times(1)).setContentType("application/xml");
        verify(httpResponse, times(1)).getWriter();
        assertThat(responseBody.toString(), stringContainsInOrder(
                Arrays.asList("Unknown request type:",
                              "&lt;any&gt;&lt;other&gt;xml&lt;/other&gt;&lt;/any&gt;")));
    }

    @Test
    public void testExecute_AnUnmashableExecuteRequest() throws Exception {
        //preparation
        when(httpRequest.getReader()).thenReturn(new BufferedReader(new StringReader("<Execute><other>xml</other></Execute>   ")));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest, httpResponse);
        Locale.setDefault(locale);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        verify(httpResponse, times(1)).setHeader("Transfer-Encoding", "chunked");
        verify(httpResponse, times(1)).setDateHeader(eq("Date"), Mockito.anyLong());
        verify(httpResponse, times(1)).setContentType("application/xml");
        verify(httpResponse, times(1)).getWriter();
        final String body = responseBody.toString();
        assertThat(body, containsString("Invalid Execute request"));
        assertThat(body, containsString("unexpected element (uri:\"\", local:\"Execute\")"));
        assertThat(body, containsString("Expected elements are &lt;{http://www.opengis.net/wps/1.0.0}AllowedValues&gt;"));
        assertThat(body, containsString("&lt;{http://www.opengis.net/wps/1.0.0}Execute&gt;"));
    }

    private String validExecute() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
               "\n" +
               "<wps:Execute xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\"\n" +
               "             service=\"WPS\"\n" +
               "             version=\"1.0.0\"\n" +
               "             xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"\n" +
               "             xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
               "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
               "\n" +
               "    <ows:Identifier>urbantep-fmask~3.2~Fmask8</ows:Identifier>\n" +
               "\n" +
               "    <wps:DataInputs>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>productionType</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>L2</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>productionName</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>TEP Fmask8 test</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>inputDataSetName</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>Landsat 8 OLI and TIRS of Finland 2013</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>minDate</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>2013-04-23</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>maxDate</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>2013-04-23</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>periodLength</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>1</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>regionWKT</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>POLYGON((-180 -90, 180 -90, 180 90, -180 90, -180 -90))</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>regionWKT</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:BoundingBoxData dimensions=\"2\">\n" +
               "                    <ows:LowerCorner>-54.58 35.532</ows:LowerCorner>\n" +
               "                    <ows:UpperCorner>-16.875 59.356</ows:UpperCorner>\n" +
               "                </wps:BoundingBoxData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "        <wps:Input>\n" +
               "            <ows:Identifier>calvalus.output.format</ows:Identifier>\n" +
               "            <wps:Data>\n" +
               "                <wps:LiteralData>NetCDF4</wps:LiteralData>\n" +
               "            </wps:Data>\n" +
               "        </wps:Input>\n" +
               "\n" +
               "\n" +
               "    </wps:DataInputs>\n" +
               "    <wps:ResponseForm>\n" +
               "        <wps:ResponseDocument storeExecuteResponse=\"true\" status=\"true\">\n" +
               "            <wps:Output>\n" +
               "                <ows:Identifier>productionResults</ows:Identifier>\n" +
               "            </wps:Output>\n" +
               "        </wps:ResponseDocument>\n" +
               "    </wps:ResponseForm>\n" +
               "</wps:Execute>";
    }
}