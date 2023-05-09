package ro.cs.tao.services.entity.beans;

import ro.cs.tao.component.enums.AuthenticationType;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.ContainerType;

import java.util.List;
import java.util.Set;

public class WebServiceBean {
    private String id;
    private String name;
    private String description;
    private ContainerType type;
    private String tag;
    private String applicationPath;
    private String logo;
    private List<Application> applications;
    private Set<String> format;
    private String commonParameters;
    private String formatNameParameter;
    private AuthenticationType authType;
    private String user;
    private String password;
    private String loginUrl;
    private String authHeader;

    public WebServiceBean() {
    }

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

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public Set<String> getFormat() {
        return format;
    }

    public void setFormat(Set<String> format) {
        this.format = format;
    }

    public String getCommonParameters() {
        return commonParameters;
    }

    public void setCommonParameters(String commonParameters) {
        this.commonParameters = commonParameters;
    }

    public String getFormatNameParameter() {
        return formatNameParameter;
    }

    public void setFormatNameParameter(String formatNameParameter) {
        this.formatNameParameter = formatNameParameter;
    }

    public AuthenticationType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthenticationType authType) {
        this.authType = authType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getAuthHeader() {
        return authHeader;
    }

    public void setAuthHeader(String authHeader) {
        this.authHeader = authHeader;
    }
}
