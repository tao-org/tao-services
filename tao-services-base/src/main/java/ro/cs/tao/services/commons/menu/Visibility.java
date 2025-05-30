package ro.cs.tao.services.commons.menu;

public enum Visibility {
    USER("#my"),
    ADMIN("#admin");

    private final String value;

    Visibility(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
