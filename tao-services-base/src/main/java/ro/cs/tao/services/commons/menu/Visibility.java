package ro.cs.tao.services.commons.menu;

/**
 * Represents the visibility level or access control for a specific component, feature,
 * or service entry in the application.
 *
 * This enumeration defines two distinct visibility levels:
 * - USER: Restricted to general users.
 * - ADMIN: Restricted to administrative users.
 *
 * Each visibility level is associated with a specific value that can be retrieved
 * using the {@code value()} method. The {@link Visibility} enumeration is primarily
 * used to define the level of access for {@link ServiceEntry} objects in the application.
 *
 * Enum Constants:
 * - USER: Represents a general user access level.
 * - ADMIN: Represents an administrator access level.
 *
 * Methods:
 * - value(): Returns the string representation of the visibility level.
 */
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
