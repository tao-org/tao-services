package ro.cs.tao.services.commons.menu;

/**
 * Represents a service entry in the application menu or navigation structure.
 * It defines key properties of the service such as its name, route, location, visibility,
 * associated section, and display position.
 *
 * This class is intended to be used as part of the application's hierarchical menu structure
 * where each service entry provides metadata needed for navigation and display.
 *
 * Key features:
 * - Supports different levels of visibility to identify users or roles that can access the service.
 * - Is associated with a specific {@link Section}, representing the broader context or grouping to which it belongs.
 * - Includes a position property to help determine the order of display within a section.
 *
 * Fields:
 * - name: The name of the service or entry, which is user-readable and identifies the service.
 * - route: The navigation route associated with this service entry.
 * - location: The location or specific context for this service in the application.
 * - visibility: An array of {@link Visibility} that specifies the access levels or roles for the service.
 * - section: The {@link Section} this service belongs to.
 * - position: A numeric value indicating the display position of the service within the section.
 *
 * Constructors:
 * - Default constructor allows creating an instance with no initial property values.
 * - Parameterized constructor allows initializing all fields of the service entry.
 *
 * Methods:
 * - getName and setName: Accessor and mutator for the name property.
 * - getRoute and setRoute: Accessor and mutator for the route property.
 * - getLocation and setLocation: Accessor and mutator for the location property.
 * - getVisibility and setVisibility: Accessor and mutator for the visibility property.
 * - getSection and setSection: Accessor and mutator for the section property.
 * - getPosition and setPosition: Accessor and mutator for the position property.
 */
public class ServiceEntry {
    private String name;
    private String route;
    private String location;
    private Visibility[] visibility;
    private Section section;
    private int position;

    public ServiceEntry() {
    }

    public ServiceEntry(String name, String route, String location, Visibility[] visibility, Section section, int position) {
        this.visibility = visibility;
        this.name = name;
        this.route = route;
        this.location = location;
        this.section = section;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public Visibility[] getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility[] visibility) {
        this.visibility = visibility;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
