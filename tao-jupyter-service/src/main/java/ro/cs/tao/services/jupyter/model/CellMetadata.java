package ro.cs.tao.services.jupyter.model;

import java.util.List;

public class CellMetadata {
    private boolean collapsed;
    private String autoscroll;
    private boolean deletable;
    private String format;
    private String name;
    private List<String> tags;

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
