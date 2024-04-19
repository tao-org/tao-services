package ro.cs.tao.services.user.impl;

import ro.cs.tao.user.*;

import java.time.LocalDateTime;
import java.util.List;

public class UserInfo {
    private String id;
    private String username;
    private String password;
    private String email;
    private String alternativeEmail;
    private String lastName;
    private String firstName;
    private String phone;
    private LocalDateTime lastLoginDate;
    private int inputQuota;
    private int actualInputQuota;
    private int processingQuota;
    private int actualProcessingQuota;
    private int cpuQuota;
    private int memoryQuota;
    private int processingTime;
    private int applicationTime;

    private String organization;
    private UserStatus status;
    private UserType userType;
    // key used for 1st time password set for TAO internal users, or for future password resets requested by TAO internal users
    private String passwordResetKey;

    private LocalDateTime created;
    private LocalDateTime expiresOn;
    private LocalDateTime modified;

    private List<Group> groups;
    private List<UserPreference> preferences;

    public UserInfo() {
    }

    public UserInfo(User user, int applicationTime, int processingTime) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.alternativeEmail = user.getAlternativeEmail();
        this.lastName = user.getLastName();
        this.firstName = user.getFirstName();
        this.phone = user.getPhone();
        this.lastLoginDate = user.getLastLoginDate();
        this.inputQuota = user.getInputQuota();
        this.actualInputQuota = user.getActualInputQuota();
        this.processingQuota = user.getProcessingQuota();
        this.actualProcessingQuota = user.getActualProcessingQuota();
        this.cpuQuota = user.getCpuQuota();
        this.memoryQuota = user.getMemoryQuota();
        this.processingTime = processingTime;
        this.applicationTime = applicationTime;
        this.organization = user.getOrganization();
        this.status = user.getStatus();
        this.userType = user.getUserType();
        this.passwordResetKey = user.getPasswordResetKey();
        this.created = user.getCreated();
        this.expiresOn = user.getExpiresOn();
        this.modified = user.getModified();
        this.groups = user.getGroups();
        this.preferences = user.getPreferences();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAlternativeEmail() {
        return alternativeEmail;
    }

    public void setAlternativeEmail(String alternativeEmail) {
        this.alternativeEmail = alternativeEmail;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public int getInputQuota() { return inputQuota; }

    public void setInputQuota(int inputQuota) { this.inputQuota = inputQuota; }

    public int getActualInputQuota() { return actualInputQuota; }

    public void setActualInputQuota(int actualInputQuota) { this.actualInputQuota = actualInputQuota; }

    public int getProcessingQuota() { return processingQuota; }

    public void setProcessingQuota(int processingQuota) { this.processingQuota = processingQuota; }

    public int getActualProcessingQuota() { return actualProcessingQuota; }

    public int getCpuQuota() { return cpuQuota; }

    public void setCpuQuota(int cpuQuota) { this.cpuQuota = cpuQuota; }

    public int getMemoryQuota() { return memoryQuota; }

    public void setMemoryQuota(int memoryQuota) { this.memoryQuota = memoryQuota; }

    public void setActualProcessingQuota(int actualProcessingQuota) { this.actualProcessingQuota = actualProcessingQuota; }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getPasswordResetKey() {
        return passwordResetKey;
    }

    public void setPasswordResetKey(String passwordResetKey) {
        this.passwordResetKey = passwordResetKey;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getExpiresOn() { return expiresOn; }

    public void setExpiresOn(LocalDateTime expiresOn) { this.expiresOn = expiresOn; }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<UserPreference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<UserPreference> preferences) {
        this.preferences = preferences;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }

    public int getApplicationTime() {
        return applicationTime;
    }

    public void setApplicationTime(int applicationTime) {
        this.applicationTime = applicationTime;
    }
}
