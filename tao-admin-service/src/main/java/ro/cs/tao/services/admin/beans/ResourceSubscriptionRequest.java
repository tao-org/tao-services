package ro.cs.tao.services.admin.beans;

import ro.cs.tao.subscription.SubscriptionType;

import java.time.LocalDateTime;
import java.util.Map;

public class ResourceSubscriptionRequest {
    private SubscriptionType type;
    private Map<String, Map<String, Integer>> flavors;
    private Integer objectStorageGB;
    private boolean paid;
    private LocalDateTime created;

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public Map<String, Map<String, Integer>> getFlavors() {
        return flavors;
    }

    public void setFlavors(Map<String, Map<String, Integer>> flavors) {
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
}
