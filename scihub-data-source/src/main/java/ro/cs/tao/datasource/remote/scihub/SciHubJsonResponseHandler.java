package ro.cs.tao.datasource.remote.scihub;

import com.fasterxml.jackson.databind.ObjectMapper;
import ro.cs.tao.datasource.common.json.JSonResponseHandler;
import ro.cs.tao.datasource.remote.scihub.json.Result;
import ro.cs.tao.datasource.util.Polygon2D;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.PixelType;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.eodata.serialization.DateAdapter;
import ro.cs.tao.eodata.serialization.GeometryAdapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public class SciHubJsonResponseHandler implements JSonResponseHandler<EOProduct> {
    @Override
    public List<EOProduct> readValues(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Result[] results = mapper.readValue(content, Result[].class);
        SentinelDownloader downloader = new SentinelDownloader("");
        return Arrays.stream(results).map(r -> {
            try {
                EOProduct product = new EOProduct();
                product.setName(r.getIdentifier());
                product.setId(r.getUuid());
                product.setSensorType(SensorType.OPTICAL);
                product.setPixelType(PixelType.UINT16);
                product.setWidth(-1);
                product.setHeight(-1);
                Polygon2D footprint = new Polygon2D();
                for (List<Double> doubleList : r.getFootprint().get(0)) {
                    footprint.append(doubleList.get(0), doubleList.get(1));
                }
                product.setGeometry(new GeometryAdapter().marshal(footprint.toWKT()));
                product.setProductType(r.getProductType());
                product.setLocation(downloader.getProductUrl(product));
                r.getIndexes().forEach(i -> i.getChildren().forEach(c -> {
                        String cName = c.getName();
                        if ("Sensing start".equals(cName)) {
                            try {
                                product.setAcquisitionDate(new DateAdapter().unmarshal(c.getValue()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (!"Footprint".equals(cName)) {
                            product.addAttribute(cName, c.getValue());
                        }
                    }));
                return product;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
    }
}
