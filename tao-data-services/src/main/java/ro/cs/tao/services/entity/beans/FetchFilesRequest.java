package ro.cs.tao.services.entity.beans;

import ro.cs.tao.component.Variable;

import java.util.List;

public class FetchFilesRequest {
    private String user;
    private String password;
    private List<Variable> urls;

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

    public List<Variable> getUrls() {
        return urls;
    }

    public void setUrls(List<Variable> urls) {
        this.urls = urls;
    }
}
