package ro.cs.tao.ogc.model.processes.core;

import java.util.List;

public class AdditionalParameter<E> {
    private String name;
    private List<E> items;

    public List<E> getItems() {
        return items;
    }

    public void setItems(List<E> items) {
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
