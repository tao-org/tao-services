package ro.cs.tao.services.entity.beans;

import ro.cs.tao.eodata.naming.NamingRule;

public class NamingRuleInfo {
    private int id;
    private String sensor;
    private String description;

    public NamingRuleInfo(NamingRule rule) {
        this.id = rule.getId();
        this.sensor = rule.getSensor();
        this.description = rule.getDescription();
    }

    public int getId() { return id; }

    public String getSensor() { return sensor; }

    public String getDescription() { return description; }
}
