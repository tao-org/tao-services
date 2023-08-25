package ro.cs.tao.services.jupyter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CellMetadata {
    private boolean collapsed = false;
    private String autoscroll = null;
    private boolean deletable = false;
    private Map<String, Object> slideshow;
    private boolean editable = false;
    private String format;
    private String name ;
    private List<String> tags = new ArrayList<>();

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public String getAutoscroll() {
        return autoscroll;
    }

    public void setAutoscroll(String autoscroll) {
        this.autoscroll = autoscroll;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public void setSlideshow(Map<String, Object> slideshow) { this.slideshow = slideshow; }

    public Map<String, Object> getSlideshow() { return this.slideshow; }

    public boolean isEditable() { return editable; }

    public void setEditable(boolean editable) { this.editable = editable; }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
