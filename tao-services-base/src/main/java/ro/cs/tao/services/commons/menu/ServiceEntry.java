package ro.cs.tao.services.commons.menu;

public class ServiceEntry {
    private String name;
    private Visibility visibility;
    private Section section;
    private int position;

    public ServiceEntry() {
    }

    public ServiceEntry(String name, Visibility visibility, Section section, int position) {
        this.visibility = visibility;
        this.name = name;
        this.section = section;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
}
