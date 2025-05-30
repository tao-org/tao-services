package ro.cs.tao.services.commons.menu;

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
