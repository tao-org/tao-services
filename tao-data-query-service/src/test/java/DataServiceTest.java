import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;

import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Cara
 */
public class DataServiceTest {

    private static EOProduct testS2Result;
    private static EOProduct testL8Result;

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testQuery1() {
        /**
         * Sample JSON query:
            {
                "dataSource": "Scientific Data Hub",
                "sensor": "Sentinel2",
                "user": "user",
                "password": "pass",
                "limit": 10,
                "pageNumber": 1,
                "pageSize": 10,
                "values": {
                    "beginPosition": "2017-11-07",
                    "endPosition": "2017-11-27",
                    "footprint": "POLYGON((20.73 43.85,24.86 43.85,24.86 46.45,20.73 46.45,20.73 43.85))",
                    "cloudcoverpercentage": 100.0
                }
            }
         */
        DataSourceComponent component = new DataSourceComponent("Sentinel2", "Amazon Web Services");
        final DataQuery query = component.createQuery();
        QueryParameter<LocalDateTime> begin = query.createParameter("beginPosition", LocalDateTime.class);
        begin.setValue(LocalDateTime.now().minusDays(15));
        query.addParameter(begin);
        Polygon2D aoi = new Polygon2D();
        aoi.append(20.738296999999992, 43.85597175445727);
        aoi.append(24.869156375, 43.85597175445727);
        aoi.append(24.869156375, 46.45834435344344);
        aoi.append(20.738296999999992, 46.45834435344344);
        aoi.append(20.738296999999992, 43.85597175445727);
        query.addParameter("footprint", aoi);

        query.addParameter("cloudcoverpercentage", 100.);
        query.setMaxResults(1);
        List<EOProduct> results = query.execute();
        Assert.notEmpty(results, "Query did not return the expected result");
        this.testS2Result = results.get(0);
    }

    @Test
    public void testQuery2() {
        /**
         * Sample JSON query:
         *
         * {
            "dataSource": "Amazon Web Services",
            "sensor": "Landsat8",
            "user": null,
            "password": null,
            "limit": 1,
            "pageNumber": 0,
            "pageSize": 0,
            "values": {
                "sensingStart": "2017-11-07",
                "footprint": "POLYGON((-9.9866909768 23.4186029838,-8.9037319257 23.4186029838,-8.9037319257 24.413397299,-9.9866909768 24.413397299,-9.9866909768 23.4186029838))",
                "cloudcoverpercentage": 100.0
            }
           }
         */

        DataSourceComponent component = new DataSourceComponent("Landsat8", "Amazon Web Services");
        final DataQuery query = component.createQuery();
        QueryParameter<LocalDateTime> begin = query.createParameter("sensingStart", LocalDateTime.class);
        begin.setValue(LocalDateTime.now().minusDays(20));
        query.addParameter(begin);
        Polygon2D aoi = new Polygon2D();
        aoi.append(-9.9866909768, 23.4186029838);
        aoi.append(-8.9037319257, 23.4186029838);
        aoi.append(-8.9037319257, 24.413397299);
        aoi.append(-9.9866909768, 24.413397299);
        aoi.append(-9.9866909768, 23.4186029838);
        query.addParameter("footprint", aoi);

        query.addParameter("cloudcoverpercentage", 100.);
        query.setMaxResults(1);
        List<EOProduct> results = query.execute();
        Assert.notEmpty(results, "Query did not return the expected result");
        this.testL8Result = results.get(0);
    }

    @Test
    public void testDownload1() {
        Assert.notNull(this.testS2Result, "Test requires a search result");
        DataSourceComponent component = new DataSourceComponent("Sentinel2", "Amazon Web Services");
        List<EOProduct> entry = new ArrayList<>();
        entry.add(this.testS2Result);
        String folder = ConfigurationManager.getInstance().getValue("products.location");
        List<EOProduct> result = component.doFetch(entry, null, folder);
        Assert.notNull(result, "Unexpected result");
        Assert.notEmpty(result, "The list should have contained one result");
        Assert.isTrue(result.size() == 1, "The list should have contained only one result");
        final String location = result.get(0).getLocation();
        Assert.notNull(location, "The location of the product should not be null");
        final URI path = URI.create(location);
        Assert.isTrue("file".equals(path.getScheme()), "Product was not downloaded");
        Assert.isTrue(Files.exists(new java.io.File(path).toPath()), "Path not found");
    }

    @Test
    public void testDownload2() {
        Assert.notNull(this.testL8Result, "Test requires a search result");
        DataSourceComponent component = new DataSourceComponent("Landsat8", "Amazon Web Services");
        List<EOProduct> entry = new ArrayList<>();
        entry.add(this.testL8Result);
        String folder = ConfigurationManager.getInstance().getValue("products.location");
        List<EOProduct> result = component.doFetch(entry, null, folder);
        Assert.notNull(result, "Unexpected result");
        Assert.notEmpty(result, "The list should have contained one result");
        Assert.isTrue(result.size() == 1, "The list should have contained only one result");
        final String location = result.get(0).getLocation();
        Assert.notNull(location, "The location of the product should not be null");
        final URI path = URI.create(location);
        Assert.isTrue(Files.exists(new java.io.File(path).toPath()), "Path not found");
    }

}
