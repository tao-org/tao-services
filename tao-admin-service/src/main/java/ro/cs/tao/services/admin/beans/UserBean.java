package ro.cs.tao.services.admin.beans;

import ro.cs.tao.user.*;

import java.time.LocalDateTime;
import java.util.List;

public class UserBean {
    private final User user;
    private int processingTime;

    public UserBean(User user) {
        this.user = user;
    }

    public String getId() {
        return user.getId();
    }

    public String getUsername() {
        return user.getUsername();
    }

    /*public String getPassword() {
        return user.getPassword();
    }*/

    public String getEmail() {
        return user.getEmail();
    }

    public String getAlternativeEmail() {
        return user.getAlternativeEmail();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getPhone() {
        return user.getPhone();
    }

    public LocalDateTime getLastLoginDate() {
        return user.getLastLoginDate();
    }

    public int getInputQuota() {
        return user.getInputQuota();
    }

    public int getActualInputQuota() {
        return user.getActualInputQuota();
    }

    public int getProcessingQuota() {
        return user.getProcessingQuota();
    }

    public int getActualProcessingQuota() {
        return user.getActualProcessingQuota();
    }

    public int getCpuQuota() {
        return user.getCpuQuota();
    }

    public int getMemoryQuota() {
        return user.getMemoryQuota();
    }

    public String getOrganization() {
        return user.getOrganization();
    }

    public UserStatus getStatus() {
        return user.getStatus();
    }

    public UserType getUserType() {
        return user.getUserType();
    }

    /*public String getPasswordResetKey() {
        return user.getPasswordResetKey();
    }*/

    public LocalDateTime getCreated() {
        return user.getCreated();
    }

    public LocalDateTime getExpiresOn() {
        return user.getExpiresOn();
    }

    public LocalDateTime getModified() {
        return user.getModified();
    }

    public List<Group> getGroups() {
        return user.getGroups();
    }

    public List<UserPreference> getPreferences() {
        return user.getPreferences();
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }
}
