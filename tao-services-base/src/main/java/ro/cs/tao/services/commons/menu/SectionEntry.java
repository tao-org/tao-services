package ro.cs.tao.services.commons.menu;

import java.util.ArrayList;
import java.util.List;

public class SectionEntry {
    private final Section section;
    private List<ServiceEntry> items;

    public SectionEntry(Section section) {
        this.section = section;
    }

    public List<ServiceEntry> getItems() {
        return items;
    }

    public void setItems(List<ServiceEntry> items) {
        this.items = items;
    }

    public void addItem(ServiceEntry entry) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(entry);
    }

    public String getTitle() {
        return this.section.getTitle();
    }

    public int getPosition() {
        return this.section.value();
    }

    @Override
    public String toString() {
        return this.section.getTitle();
    }
}
