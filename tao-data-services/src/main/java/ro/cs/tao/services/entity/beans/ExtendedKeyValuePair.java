package ro.cs.tao.services.entity.beans;

import ro.cs.tao.services.model.KeyValuePair;

/**
 * ExtendedKeyValuePair is a class that extends the KeyValuePair class
 * by including an additional boolean property that specifies whether
 * the key-value pair is user-visible. This allows for finer control
 * over which key-value pairs are exposed to the user.
 *
 * Inherits functionality from the KeyValuePair class for managing
 * a key and value but adds the ability to manage visibility status.
 */
public class ExtendedKeyValuePair extends KeyValuePair {
    private boolean userVisible;

    public ExtendedKeyValuePair(String key, String value, boolean userVisible) {
        super(key, value);
        this.userVisible = userVisible;
    }

    public boolean isUserVisible() {
        return userVisible;
    }

    public void setUserVisible(boolean userVisible) {
        this.userVisible = userVisible;
    }
}
