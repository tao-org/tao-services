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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public class WPSControllerTest_Execute {

    private HttpServletRequest httpRequest;
    private WPSController wpsController;
    private WpsServiceInstance wpsServiceInstance;

    @Before
    public void setUp() throws Exception {
        httpRequest = mock(HttpServletRequest.class);
        wpsServiceInstance = mock(WpsServiceInstance.class);

        // class under test
        wpsController = new WPSController();
        ReflectionTestUtils.setField(wpsController, "wpsServiceInstance", wpsServiceInstance);
    }

    @Test
    public void testExecute_ValidRequest() throws Exception {
        //preparation
        when(httpRequest.getInputStream()).thenReturn(new TestStream(validExecute()));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
        final HttpHeaders headers = responseEntity.getHeaders();
        assertThat(headers.size(), is(1));
        assertThat(headers.getContentType(), is(equalTo(MediaType.APPLICATION_XML)));
        assertThat((String) responseEntity.getBody(), is(equalToIgnoringWhiteSpace(
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
        when(httpRequest.getInputStream()).thenReturn(new TestStream("<any><other>xml</other></any>   "));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.SERVICE_UNAVAILABLE)));
        assertThat(responseEntity.getHeaders().size(), is(0));
        assertThat((String) responseEntity.getBody(), is(equalToIgnoringWhiteSpace("Unknown request type: \"<any><other>xml</other></any>\"")));
    }

    @Test
    public void testExecute_AnUnmashableExecuteRequest() throws Exception {
        //preparation
        when(httpRequest.getInputStream()).thenReturn(new TestStream("<Execute><other>xml</other></Execute>   "));

        final StatusType statusType = new StatusType();
        statusType.setProcessAccepted("AAAAAAAAAAAA");
        final ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStatus(statusType);
        when(wpsServiceInstance.doExecute(any(WpsRequestContext.class), any(Execute.class)))
                .thenReturn(executeResponse);

        //execution
        final Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        final ResponseEntity<?> responseEntity = wpsController.requestPost(httpRequest);
        Locale.setDefault(locale);

        //verification
        assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.SERVICE_UNAVAILABLE)));
        assertThat(responseEntity.getHeaders().size(), is(0));
        final String body = (String) responseEntity.getBody();
        assertThat(body, containsString("Invalid Execute request"));
        assertThat(body, containsString("unexpected element (uri:\"\", local:\"Execute\")"));
        assertThat(body, containsString("Expected elements are <{http://www.opengis.net/wps/1.0.0}AllowedValues>"));
        assertThat(body, containsString("<{http://www.opengis.net/wps/1.0.0}Execute>"));
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

    private class TestStream extends ServletInputStream {

        final ByteArrayInputStream is;

        public TestStream(final String xml) {
            is = new ByteArrayInputStream(xml.getBytes());
        }

        @Override
        public boolean isFinished() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean isReady() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public long skip(long n) throws IOException {
            return is.skip(n);  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public int available() throws IOException {
            return is.available();  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public void close() throws IOException {
            is.close();  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public synchronized void mark(int readlimit) {
            is.mark(readlimit);  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public synchronized void reset() throws IOException {
            is.reset();  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        @Override
        public boolean markSupported() {
            return is.markSupported();  //Todo change body of created method. Use File | Settings | File Templates to change
        }
    }
}