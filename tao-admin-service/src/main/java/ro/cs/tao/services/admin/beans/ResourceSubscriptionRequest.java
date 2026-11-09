package ro.cs.tao.services.admin.beans;

import ro.cs.tao.subscription.SubscriptionType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a request for resource subscription, encompassing various parameters
 * such as subscription type, resource flavors, object storage size, payment status,
 * and creation date.
 *
 * This class is designed to handle the details of a subscription request for resources
 * required by users in a system. It facilitates the setup and management of subscription
 * configurations, supporting features like different resource flavors and storage allocations.
 *
 * Key functionalities include:
 * - Defining the subscription type (e.g., fixed resources or pay-per-use)
 * - Specifying the resource flavors with their configurations
 * - Allocating object storage in GB
 * - Indicating whether the subscription is paid or unpaid
 * - Tracking the creation timestamp of the subscription request
 */
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
