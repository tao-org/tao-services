package ro.cs.eo.gdal.reader;

public class Extent {
    public double xmin;
    public double ymin;
    public double xmax;
    public double ymax;

    public Extent() {
    }

    public Extent(double xmin, double ymin, double xmax, double ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }
}
