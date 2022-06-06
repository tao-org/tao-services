package ro.cs.eo.gdal.reader;


public final class ImageInfo {

    private static final String NAME_KEY = "name";
    private static final String PROJECTION_KEY = "projection";
    private static final String CODE_KEY = "code";
    private static final String UNIT_KEY = "unit";
    private static final String EXTENT_KEY = "extent";
    private static final String TILE_GRID_KEY = "tile_grid";
    private static final String RESOLUTIONS_KEY = "resolutions";
    private static final String MIN_ZOOM_KEY = "min_zoom";
    private static final String MAX_ZOOM_KEY = "max_zoom";
    private static final String CENTER_KEY = "center";

    private final String name;
    private final String projectionCode;
    private final String projectionUnit;
    private final String projectionExtent;
    private final String extent;
    private final String tileGrid;
    private final String resolutions;
    private final String minZoom;
    private final String maxZoom;
    private final String center;

    ImageInfo(String name, String projectionCode, String projectionUnit, String projectionExtent, String tileGrid, String extent, String resolutions, String minZoom, String maxZoom, String center) {
        this.name = name;
        this.projectionCode = projectionCode;
        this.projectionUnit = projectionUnit;
        this.projectionExtent = projectionExtent;
        this.tileGrid = tileGrid;
        this.extent = extent;
        this.resolutions = resolutions;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.center = center;
    }

    public String toJson() {
        return "{"
                + "\"" + NAME_KEY + "\": \"" + this.name + "\", "
                + "\"" + PROJECTION_KEY + "\": "
                + "{"
                + "\"" + CODE_KEY + "\": \"" + this.projectionCode + "\", "
                + "\"" + UNIT_KEY + "\": \"" + this.projectionUnit + "\", "
                + "\"" + EXTENT_KEY + "\": " + this.projectionExtent + "}, "
                + "\"" + TILE_GRID_KEY + "\": "
                + "{"
                + "\"" + NAME_KEY + "\": \"" + this.tileGrid + "\""
                + (this.resolutions.isEmpty()
                ? "}}"
                : ", \"" + EXTENT_KEY + "\": " + this.extent + ", "
                + "\"" + RESOLUTIONS_KEY + "\": " + this.resolutions + ", "
                + "\"" + MIN_ZOOM_KEY + "\": " + this.minZoom + ", "
                + "\"" + MAX_ZOOM_KEY + "\": " + this.maxZoom + "}, "
                + "\"" + CENTER_KEY + "\": " + this.center + "}");
    }
}
