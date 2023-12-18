package ro.cs.eo.gdal.service;

import ro.cs.eo.gdal.reader.Extent;

import java.nio.file.Path;
import java.util.List;

public interface FileGeometryFilter {

    Extent computeEnvelope(String geometryWKT);
    List<Path> filter(String geometryWKT);
}
