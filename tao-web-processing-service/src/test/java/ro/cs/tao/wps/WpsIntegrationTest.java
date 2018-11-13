package ro.cs.tao.wps;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static ro.cs.tao.wps.IntegrationTestHelper.getHttpGetResponse;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.junit.runner.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(IntegrationTestRunner.class)
@IntegrationTestRunner.NeededService("http://localhost:8080/wps")
public class WpsIntegrationTest {

    final static String serviceAdress;

    static {
        final IntegrationTestRunner.NeededService annotation
                = WpsIntegrationTest.class.getAnnotation(IntegrationTestRunner.NeededService.class);
        serviceAdress = annotation.value();
    }

    CloseableHttpResponse closeableHttpResponse;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        HttpClientUtils.closeQuietly(closeableHttpResponse);
        closeableHttpResponse.close();
    }

    @Test
    public void testGetCapabilities_withMock() throws IOException {
        closeableHttpResponse = getHttpGetResponse(serviceAdress + "?Service=WPS&Request=GetCapabilities");

        assertThat(closeableHttpResponse, is(notNullValue()));
        final StatusLine statusLine = closeableHttpResponse.getStatusLine();
        assertThat(statusLine.getStatusCode(), is(equalTo(202)));
        assertThat(statusLine.getReasonPhrase(), is(equalTo("Accepted")));
        final HttpEntity entity = closeableHttpResponse.getEntity();
        assertThat(entity, is(notNullValue()));
        final String body = EntityUtils.toString(entity);

        assertThat(body, is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wps:Capabilities ns2:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsGetCapabilities_response.xsd\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:ns1=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns2=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.0.0\" service=\"WPS\" xml:lang=\"en\">\n" +
                "   <ows:ServiceIdentification>\n" +
                "      <ows:Title>TAO WPS server</ows:Title>\n" +
                "      <ows:Abstract>Web Processing Service for TAO</ows:Abstract>\n" +
                "      <ows:ServiceType>WPS</ows:ServiceType>\n" +
                "      <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>\n" +
                "   </ows:ServiceIdentification>\n" +
                "   <ows:ServiceProvider>\n" +
                "      <ows:ProviderName>CS ROMANIA S. A.</ows:ProviderName>\n" +
                "      <ows:ProviderSite xlink:href=\"http://www.c-s.ro/\"/>\n" +
                "      <ows:ServiceContact>\n" +
                "         <ows:IndividualName>Cosmin Cara</ows:IndividualName>\n" +
                "         <ows:PositionName>Project Manager</ows:PositionName>\n" +
                "         <ows:ContactInfo>\n" +
                "            <ows:Phone>\n" +
                "               <ows:Voice>+40-(0)251-41 28 50 or +40-(0)351-41 58 88</ows:Voice>\n" +
                "               <ows:Facsimile>+40-(0)251-41 73 07</ows:Facsimile>\n" +
                "            </ows:Phone>\n" +
                "            <ows:Address>\n" +
                "               <ows:DeliveryPoint>29 Pacii street</ows:DeliveryPoint>\n" +
                "               <ows:City>Craiovay</ows:City>\n" +
                "               <ows:AdministrativeArea>DOLJ</ows:AdministrativeArea>\n" +
                "               <ows:PostalCode>200692</ows:PostalCode>\n" +
                "               <ows:Country>Romania</ows:Country>\n" +
                "               <ows:ElectronicMailAddress>office@c-s.ro</ows:ElectronicMailAddress>\n" +
                "            </ows:Address>\n" +
                "            <ows:OnlineResource xlink:href=\"http://www.c-s.ro/\"/>\n" +
                "            <ows:HoursOfService>24x7</ows:HoursOfService>\n" +
                "            <ows:ContactInstructions>Don't hesitate to call</ows:ContactInstructions>\n" +
                "         </ows:ContactInfo>\n" +
                "         <ows:Role>PointOfContact</ows:Role>\n" +
                "      </ows:ServiceContact>\n" +
                "   </ows:ServiceProvider>\n" +
                "   <ows:OperationsMetadata>\n" +
                "      <ows:Operation name=\"GetCapabilities\">\n" +
                "         <ows:DCP>\n" +
                "            <ows:HTTP>\n" +
                "               <ows:Get xlink:href=\"http://?????????????/wps?\"/>\n" +
                "            </ows:HTTP>\n" +
                "         </ows:DCP>\n" +
                "      </ows:Operation>\n" +
                "      <ows:Operation name=\"DescribeProcess\">\n" +
                "         <ows:DCP>\n" +
                "            <ows:HTTP>\n" +
                "               <ows:Get xlink:href=\"http://?????????????/wps?\"/>\n" +
                "            </ows:HTTP>\n" +
                "         </ows:DCP>\n" +
                "      </ows:Operation>\n" +
                "      <ows:Operation name=\"Execute\">\n" +
                "         <ows:DCP>\n" +
                "            <ows:HTTP>\n" +
                "               <ows:Post xlink:href=\"http://?????????????/wps\"/>\n" +
                "            </ows:HTTP>\n" +
                "         </ows:DCP>\n" +
                "      </ows:Operation>\n" +
                "      <ows:Operation name=\"GetStatus\">\n" +
                "         <ows:DCP>\n" +
                "            <ows:HTTP>\n" +
                "               <ows:Get xlink:href=\"http://?????????????/wps?\"/>\n" +
                "            </ows:HTTP>\n" +
                "         </ows:DCP>\n" +
                "      </ows:Operation>\n" +
                "   </ows:OperationsMetadata>\n" +
                "   <wps:ProcessOfferings>\n" +
                "      <wps:Process>\n" +
                "         <ows:Identifier>1</ows:Identifier>\n" +
                "         <ows:Title>OTB Resample, NDVI, TNDVI and Concatenate</ows:Title>\n" +
                "      </wps:Process>\n" +
                "      <wps:Process>\n" +
                "         <ows:Identifier>2</ows:Identifier>\n" +
                "         <ows:Title>OTB Radiometric Indices + OTB RESAMPLE workflow</ows:Title>\n" +
                "      </wps:Process>\n" +
                "   </wps:ProcessOfferings>\n" +
                "   <wps:Languages>\n" +
                "      <wps:Default>\n" +
                "         <ows:Language>EN</ows:Language>\n" +
                "      </wps:Default>\n" +
                "      <wps:Supported>\n" +
                "         <ows:Language>EN</ows:Language>\n" +
                "      </wps:Supported>\n" +
                "   </wps:Languages>\n" +
                "</wps:Capabilities>")));
    }

    @Test
    public void testDescribeProcess_processOne_withMock() throws IOException {
        closeableHttpResponse = getHttpGetResponse(serviceAdress + "?Service=WPS&Request=DescribeProcess&Version=1.0.0&Identifier=1");

        assertThat(closeableHttpResponse, is(notNullValue()));
        final StatusLine statusLine = closeableHttpResponse.getStatusLine();
        assertThat(statusLine.getStatusCode(), is(equalTo(202)));
        assertThat(statusLine.getReasonPhrase(), is(equalTo("Accepted")));

        final HttpEntity entity = closeableHttpResponse.getEntity();
        assertThat(entity, is(notNullValue()));
        final String body = EntityUtils.toString(entity);

        assertThat(body, is(equalToIgnoringWhiteSpace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wps:ProcessDescriptions ns2:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsDescribeProcess_response.xsd\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:ns1=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns2=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" service=\"WPS\" version=\"1.0.0\" xml:lang=\"en\">\n" +
                "   <ProcessDescription wps:processVersion=\"na\">\n" +
                "      <ows:Identifier>1</ows:Identifier>\n" +
                "      <ows:Title>TAO Workflow ... 1</ows:Title>\n" +
                "      <DataInputs>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~interpolator_bco_radius_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'interpolator_bco_radius_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'interpolator_bco_radius_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~interpolator_str</ows:Identifier>\n" +
                "            <ows:Title>Param 'interpolator_str' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'interpolator_str' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.String</ows:DataType>\n" +
                "               <ows:AllowedValues>\n" +
                "                  <ows:Value>nn</ows:Value>\n" +
                "                  <ows:Value>linear</ows:Value>\n" +
                "                  <ows:Value>bco</ows:Value>\n" +
                "               </ows:AllowedValues>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_id_scalex_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_id_scalex_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_id_scalex_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_id_scaley_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_id_scaley_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_id_scaley_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_rotation_angle_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_rotation_angle_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_rotation_angle_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_rotation_scalex_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_rotation_scalex_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_rotation_scalex_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_rotation_scaley_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_rotation_scaley_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_rotation_scaley_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_str</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_str' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_str' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.String</ows:DataType>\n" +
                "               <ows:AllowedValues>\n" +
                "                  <ows:Value>id</ows:Value>\n" +
                "                  <ows:Value>translation</ows:Value>\n" +
                "                  <ows:Value>rotation</ows:Value>\n" +
                "               </ows:AllowedValues>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_translation_scalex_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_translation_scalex_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_translation_scalex_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_translation_scaley_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_translation_scaley_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_translation_scaley_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_translation_tx_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_translation_tx_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_translation_tx_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB Resample~transform_type_translation_ty_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'transform_type_translation_ty_number' of group 'OTB Resample'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'transform_type_translation_ty_number' of parametergroup 'OTB Resample'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Float</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~channels_blue_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'channels_blue_number' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'channels_blue_number' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~channels_green_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'channels_green_number' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'channels_green_number' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~channels_mir_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'channels_mir_number' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'channels_mir_number' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~channels_nir_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'channels_nir_number' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'channels_nir_number' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~channels_red_number</ows:Identifier>\n" +
                "            <ows:Title>Param 'channels_red_number' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'channels_red_number' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.Integer</ows:DataType>\n" +
                "               <ows:AnyValue/>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "         <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                "            <ows:Identifier>OTB NDVI~list_str</ows:Identifier>\n" +
                "            <ows:Title>Param 'list_str' of group 'OTB NDVI'.</ows:Title>\n" +
                "            <ows:Abstract>The parameter 'list_str' of parametergroup 'OTB NDVI'.</ows:Abstract>\n" +
                "            <LiteralData>\n" +
                "               <ows:DataType>java.lang.String</ows:DataType>\n" +
                "               <ows:AllowedValues>\n" +
                "                  <ows:Value>ndvi</ows:Value>\n" +
                "                  <ows:Value>tndvi</ows:Value>\n" +
                "                  <ows:Value>rvi</ows:Value>\n" +
                "                  <ows:Value>savi</ows:Value>\n" +
                "                  <ows:Value>tsavi</ows:Value>\n" +
                "                  <ows:Value>msavi</ows:Value>\n" +
                "                  <ows:Value>msavi2</ows:Value>\n" +
                "                  <ows:Value>gemi</ows:Value>\n" +
                "                  <ows:Value>ipvi</ows:Value>\n" +
                "                  <ows:Value>ndwi</ows:Value>\n" +
                "                  <ows:Value>ndwi2</ows:Value>\n" +
                "                  <ows:Value>mndwi</ows:Value>\n" +
                "                  <ows:Value>ndpi</ows:Value>\n" +
                "                  <ows:Value>ndti</ows:Value>\n" +
                "                  <ows:Value>ri</ows:Value>\n" +
                "                  <ows:Value>ci</ows:Value>\n" +
                "                  <ows:Value>bi</ows:Value>\n" +
                "                  <ows:Value>bi2</ows:Value>\n" +
                "               </ows:AllowedValues>\n" +
                "            </LiteralData>\n" +
                "         </Input>\n" +
                "      </DataInputs>\n" +
                "      <ProcessOutputs>\n" +
                "         <Output>\n" +
                "            <ows:Identifier>9e3910c9-1ca3-4e7a-a888-844699b32022</ows:Identifier>\n" +
                "            <ows:Title>output_otbcli_ConcatenateImages.tif</ows:Title>\n" +
                "            <ComplexOutput>\n" +
                "               <Default>\n" +
                "                  <Format>\n" +
                "                     <MimeType>application/octet-stream</MimeType>\n" +
                "                  </Format>\n" +
                "               </Default>\n" +
                "               <Supported>\n" +
                "                  <Format>\n" +
                "                     <MimeType>application/octet-stream</MimeType>\n" +
                "                  </Format>\n" +
                "               </Supported>\n" +
                "            </ComplexOutput>\n" +
                "         </Output>\n" +
                "      </ProcessOutputs>\n" +
                "   </ProcessDescription>\n" +
                "</wps:ProcessDescriptions>\n")));
    }

    @Test
    public void testExecute() throws IOException {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
        pw.println("<wps:Execute xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\"");
        pw.println("             service=\"WPS\"");
        pw.println("             version=\"1.0.0\"");
        pw.println("             xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"");
        pw.println("             xmlns:ows=\"http://www.opengis.net/ows/1.1\"");
        pw.println("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        pw.println("    <ows:Identifier>1</ows:Identifier>");
        pw.println("    <wps:DataInputs>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~interpolator_bco_radius_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>5</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~interpolator_str</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>linear</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_id_scalex_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.1</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_id_scaley_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.2</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_rotation_angle_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.3</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_rotation_scalex_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.4</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_rotation_scaley_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.5</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_str</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>id</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_translation_scalex_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.6</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_translation_scaley_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.7</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_translation_tx_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.8</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB Resample~transform_type_translation_ty_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>1.9</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~channels_blue_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>11</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~channels_green_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>12</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~channels_mir_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>13</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~channels_nir_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>14</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~channels_red_number</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>15</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("        <wps:Input>");
        pw.println("            <ows:Identifier>OTB NDVI~list_str</ows:Identifier>");
        pw.println("            <wps:Data>");
        pw.println("                <wps:LiteralData>ndvi</wps:LiteralData>");
        pw.println("            </wps:Data>");
        pw.println("        </wps:Input>");
        pw.println("    </wps:DataInputs>");
        pw.println("    <wps:ResponseForm>");
        pw.println("        <wps:ResponseDocument storeExecuteResponse=\"true\" status=\"true\">");
        pw.println("            <wps:Output>");
        pw.println("                <ows:Identifier>productionResults</ows:Identifier>");
        pw.println("            </wps:Output>");
        pw.println("        </wps:ResponseDocument>");
        pw.println("    </wps:ResponseForm>");
        pw.println("</wps:Execute>");
        pw.flush();
        sw.flush();
        final String xml = sw.toString();

        closeableHttpResponse = IntegrationTestHelper.getWpsExecuteResponse(serviceAdress + "?Service=WPS&Request=Execute&Version=1.0.0&Identifier=1", xml);

        assertThat(closeableHttpResponse, is(notNullValue()));
    }
}
