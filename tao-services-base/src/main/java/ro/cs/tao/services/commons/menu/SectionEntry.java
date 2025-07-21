package ro.cs.tao.services.commons.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry within a specific section of the application.
 * A section entry contains a reference to a {@link Section} and a list of {@link ServiceEntry} items
 * associated with that section. It provides methods to manage and access the
 * items and section-specific details.
 *
 * The main responsibilities of this class include:
 * - Linking a section with its associated service entries.
 * - Providing access to the section's title and position.
 * - Allowing addition of new service entries to the section.
 *
 * The section is immutable, while the list of service entries can be modified.
 */
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
