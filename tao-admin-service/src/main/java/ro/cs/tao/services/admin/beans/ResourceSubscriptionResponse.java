package ro.cs.tao.services.admin.beans;

import ro.cs.tao.subscription.SubscriptionType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a response for a resource subscription, providing details about
 * the subscription configuration, status, and timeline.
 *
 * This class is used to encapsulate information about a user's resource subscription,
 * including the type of subscription, associated flavors, storage allocation, and
 * payment status, along with the subscription's creation and end timestamps.
 *
 * Key functionalities include:
 * - Retrieving and setting the user ID associated with the subscription.
 * - Getting and defining the subscription type (e.g., fixed resources or pay-per-use).
 * - Managing the list of resource flavors involved in the subscription.
 * - Retrieving and updating the allocated storage in gigabytes.
 * - Managing the payment status of the subscription.
 * - Tracking the creation time and end time of the subscription.
 */
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
