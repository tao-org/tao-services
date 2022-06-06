package ro.cs.eo.gdal.reader;

public class Config {

    private static final double EQUATOR_LENGTH = 40075.016686;

    final TileGrid tileGrid;
    final boolean reverseY;
    final long tileWidth;
    final long tileHeight;
    final double mapResolution;

    private Config(TileGrid tileGrid, boolean reverseY, long tileWidth, long tileHeight) {
        this.tileGrid = tileGrid;
        this.reverseY = reverseY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.mapResolution = EQUATOR_LENGTH * 1000 / tileWidth;
    }

    static Config getWebMercatorConfig() {
        return new Config(TileGrid.webMercator(), false, 256, 256);
    }

    static Config getCustomConfig(TileGrid tileGrid) {
        return new Config(tileGrid, false, 256, 256);
    }
}
