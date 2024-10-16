package ro.cs.tao.ogc.model.processes.core;

import java.util.List;

public class BBox {
    private List<Double> bbox;

    public List<Double> getBbox() {
        return bbox;
    }

    public void setBbox(List<Double> bbox) {
        if (bbox != null && (bbox.size() != 4 && bbox.size() != 6)) {
            throw new IllegalArgumentException("Must have either 4 or 6 values");
        }
        this.bbox = bbox;
    }
}
