package ro.cs.tao.ogc.model.common;

import java.util.ArrayList;
import java.util.List;

public class ConfClasses {
    private List<String> conformsTo;

    public List<String> getConformsTo() {
        return conformsTo;
    }

    public void setConformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    public void addConformance(String conformsTo) {
        if (this.conformsTo == null) {
            this.conformsTo = new ArrayList<>();
        }
        this.conformsTo.add(conformsTo);
    }
}
