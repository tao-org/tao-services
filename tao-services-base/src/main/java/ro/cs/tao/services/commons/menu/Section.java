package ro.cs.tao.services.commons.menu;

import ro.cs.tao.TaoEnum;

/**
 * Represents distinct sections within the application, each associated with a title
 * and a position to determine its order in the user interface or other logic.
 *
 * Each section has a human-readable title along with an integer position value.
 * The enumeration implements {@code TaoEnum<Integer>} to provide additional functionality,
 * including a friendly name and value retrieval.
 *
 * The available sections are:
 * - DASHBOARD: Represents the main dashboard area.
 * - DATASETS: Represents the user's datasets area.
 * - WORKFLOWS: Represents workflows for the user.
 * - COMPONENTS: Represents available components for the user.
 * - ADMINISTRATION: Represents the administration area.
 */
public enum Section implements TaoEnum<Integer> {
    DASHBOARD("Dashboard", 1),
    DATASETS("My Datasets", 2),
    WORKFLOWS("My Workflows", 3),
    COMPONENTS("My Components", 4),
    ADMINISTRATION("Admin area", 5);


    private final String title;
    private final int position;

    Section(String value, int position) {
        this.title = value;
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String friendlyName() {
        return title;
    }

    @Override
    public Integer value() {
        return position;
    }
}
