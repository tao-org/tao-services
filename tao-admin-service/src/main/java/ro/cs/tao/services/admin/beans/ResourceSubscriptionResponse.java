package ro.cs.tao.services.admin.beans;

import ro.cs.tao.subscription.SubscriptionType;

import java.time.LocalDateTime;
import java.util.List;

public class ResourceSubscriptionResponse {
    private String userId;
    private SubscriptionType type;
    private List<Flavor> flavors;
    private Integer objectStorageGB;
    private boolean paid;
    private LocalDateTime created;
    private LocalDateTime ended;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public List<Flavor> getFlavors() {
        return flavors;
    }

    public void setFlavors(List<Flavor> flavors) {
        this.flavors = flavors;
    }

    public Integer getObjectStorageGB() {
        return objectStorageGB;
    }

    public void setObjectStorageGB(Integer objectStorageGB) {
        this.objectStorageGB = objectStorageGB;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getEnded() {
        return ended;
    }

    public void setEnded(LocalDateTime ended) {
        this.ended = ended;
    }

}
