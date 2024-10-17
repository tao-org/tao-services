package ro.cs.tao.ogc.model.common;

import java.util.ArrayList;
import java.util.List;

public class LandingPage {
    private String title;
    private String description;
    private String attribution;
    private List<Link> links;

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        if (this.links == null) {
            this.links = new ArrayList<>();
        }
        this.links.add(link);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
