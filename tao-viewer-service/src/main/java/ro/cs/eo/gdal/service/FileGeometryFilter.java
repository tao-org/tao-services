package ro.cs.eo.gdal.service;

import ro.cs.eo.gdal.reader.Extent;

import java.nio.file.Path;
import java.util.List;

public interface FileGeometryFilter {

    default Extent computeEnvelope(String geometryWKT) {
        return computeEnvelope(geometryWKT, 0.01);
    }
    Extent computeEnvelope(String geometryWKT, double buffer);

    List<Path> filter(String geometryWKT);
}
