package ro.cs.tao.services.query.model;

import java.util.Map;

/**
 * @author Cosmin Cara
 */
public class Query {
    private String user;
    private String password;
    private Map<String, Object> values;

    public Query() { }

    public Map<String, Object> getValues() { return values; }

    public void setValues(Map<String, Object> values) { this.values = values; }

    public String getUser() { return user; }

    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
