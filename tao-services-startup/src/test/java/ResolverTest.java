import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ro.cs.tao.functions.DateTimeResolver;
import ro.cs.tao.functions.ProjectionResolver;
import ro.cs.tao.functions.StringResolver;
import ro.cs.tao.products.sentinels.FootprintResolver;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResolverTest {

    @Test
    void testDateFunctions() {
        DateTimeResolver resolver = new DateTimeResolver();
        final String expression = "YEAR(20221201)-" +
                "MONTH(20221201)-" +
                "DAY(20221201)T" +
                "HOUR(091239):" +
                "MINUTE(091239):" +
                "SECOND(091239)";
        Assert.assertEquals("2022-12-01T09:12:39", resolver.resolve(expression));
    }

    @Test
    void testProjectionFunction() {
        ProjectionResolver resolver = new ProjectionResolver();
        final String expression = "EPSG_CODE(35TLH)";
        Assert.assertEquals("EPSG:32635", resolver.resolve(expression));
    }

    @Test
    void testFootprintFunctions() {
        FootprintResolver s2Resolver = new FootprintResolver();
        String expression = "FOOTPRINT(35TLH)";
        Assert.assertEquals("POLYGON((24.533218 43.326246,25.887078 43.34744,25.904691 42.35884,24.572219 42.338362,24.533218 43.326246))",
                            s2Resolver.resolve(expression));
        ro.cs.tao.products.landsat.FootprintResolver landsatResolver = new ro.cs.tao.products.landsat.FootprintResolver();
        expression = "FOOTPRINT(185031)";
        Assert.assertEquals("POLYGON((20.458 42.718,22.767 42.382,22.215 40.793,19.959 41.121,20.458 42.718))",
                            landsatResolver.resolve(expression));
    }

    @Test
    void testStringFunctions() {
        StringResolver resolver = new StringResolver();
        final String name = "S2B_MSIL1C_20220501T093029_N0400_R136_T34TEP_20220501T102949.SAFE";
        String expression = "SUBSTRING(" + name + ",2,1)";
        Assert.assertEquals("B", resolver.resolve(expression));
        expression = "LOWER(" + name + ")";
        Assert.assertEquals(name.toLowerCase(), resolver.resolve(expression));
    }
}
