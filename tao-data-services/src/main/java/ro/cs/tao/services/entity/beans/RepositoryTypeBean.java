package ro.cs.tao.services.entity.beans;

import java.util.Map;

public class RepositoryTypeBean {
    private int id;
    private String name;
    private String description;
    private String urlPrefix;
    private boolean singleton;
    private String rootKey;
    private Map<String, Boolean> parameters;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public void setUrlPrefix(String prefix) {
        this.urlPrefix = prefix;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public Map<String, Boolean> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Boolean> parameters) {
        this.parameters = parameters;
    }
}
