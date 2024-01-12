package ro.cs.tao.services.entity.beans;

import java.util.LinkedHashMap;
import java.util.List;

public class RepositoryBean {
    private String id;
    private String name;
    private String description;
    private String urlPrefix;
    private String rootKey;
    private String type;
    private boolean system;
    private boolean readOnly;
    private boolean editable;
    private String pageItem;
    private short order;
    private boolean persistent;
    private LinkedHashMap<String, String> parameters;
    private List<Action> actions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRootKey() {
        return rootKey;
    }

    public void setRootKey(String rootKey) {
        this.rootKey = rootKey;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(short order) {
        this.order = order;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public String getPageItem() {
        return this.pageItem;
    }

    public void setPageItem(final String pageItem) {
        this.pageItem = pageItem;
    }

    public LinkedHashMap<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(LinkedHashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public static class Action {
        private String name;
        private String[] filters;

        public Action() {
        }

        public Action(String name, String[] filters) {
            this.name = name;
            this.filters = filters;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String[] getFilters() {
            return filters;
        }

        public void setFilters(String[] filters) {
            this.filters = filters;
        }
    }
}
