package ro.cs.tao.services.progress.impl;

public class Filter {
    private String name;
    private String value;

    public Filter() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{\"name\":\"" + name + "\",\"value\":\"" + value + "\"}";
    }
}
