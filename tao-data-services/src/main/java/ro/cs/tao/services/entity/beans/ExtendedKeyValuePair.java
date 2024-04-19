package ro.cs.tao.services.entity.beans;

import ro.cs.tao.services.model.KeyValuePair;

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
