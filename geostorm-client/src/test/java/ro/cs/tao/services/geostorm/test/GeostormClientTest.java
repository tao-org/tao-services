package ro.cs.tao.services.geostorm.test;

import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.geotools.geojson.geom.GeometryJSON;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.services.geostorm.GeostormClient;
import ro.cs.tao.services.geostorm.model.Resource;

import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Tests for GeostormClient
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:geostorm-client-context.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeostormClientTest {

    private static Log log = LogFactory.getLog(GeostormClientTest.class);

    @Autowired
    private GeostormClient geostormClient;

    @Test
    public void T_01_addResource()
    {
        log.info("test addResource....");

        Resource resource = new Resource();

        // set first the mandatory fields
        resource.setExecution_id(0);
        resource.setData_path("//myPath");
        resource.setData_type("output");
        resource.setName("S2_Dummy_Product");
        resource.setShort_description("Sentinel 2 Granule S2_Dummy_Product");
        resource.setOrganisation("CS");
        resource.setUsage("Free");

        resource.setEntry_point("aaa");
        resource.setResource_storage_type("process_result");

        resource.setRelease_date("2018-03-08");
        resource.setCollection("Sentinel_2");
        /*resource.setWkb_geometry("{\"type\": \"Polygon\",\n" +
          "            \"coordinates\": [[[143.503571067188, 44.2259751275381],\n" +
          "                             [143.462735477681, 43.2382731563042],\n" +
          "                             [144.812631659451, 43.2012113546965],\n" +
          "                             [144.875738946256, 44.1876215835231],\n" +
          "                             [143.503571067188, 44.2259751275381]]]}");*/

        // works (the field is optional)
        //resource.setWkb_geometry(null);

        GeometryJSON gtjson = new GeometryJSON();
        Polygon polygon = null;
        try {
            polygon = gtjson.readPolygon(new StringReader("{\"type\": \"Polygon\",\n" +
              "            \"coordinates\": [[[143.503571067188, 44.2259751275381],\n" +
              "                             [143.462735477681, 43.2382731563042],\n" +
              "                             [144.812631659451, 43.2012113546965],\n" +
              "                             [144.875738946256, 44.1876215835231],\n" +
              "                             [143.503571067188, 44.2259751275381]]]}"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        resource.setWkb_geometry("{\"type\": \"Polygon\",\n" +
          "            \"coordinates\": [[[143.503571067188, 44.2259751275381],\n" +
          "                             [143.462735477681, 43.2382731563042],\n" +
          "                             [144.812631659451, 43.2012113546965],\n" +
          "                             [144.875738946256, 44.1876215835231],\n" +
          "                             [143.503571067188, 44.2259751275381]]]}");


        Integer[] coord_sys = {4326};
        resource.setCoord_sys(coord_sys);

        geostormClient.addResource(resource);
    }

    @Test
    public void T_02_getResources()
    {
        log.info("test getResources ...");

        log.info(geostormClient.getResources());
    }
}
