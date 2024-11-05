package ro.cs.tao.ogc.model.processes.core;

import java.util.List;

public class ListDescriptor extends Descriptor<List<String>> {
    private int minItems;
    private int maxItems;
    private boolean uniqueItems;
    private List<String> items;

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        if (items != null && items.isEmpty()) {
            throw new IllegalArgumentException("At least 1 item required");
        }
        this.items = items;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getMinItems() {
        return minItems;
    }

    public void setMinItems(int minItems) {
        this.minItems = minItems;
    }

    public boolean isUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }
}
