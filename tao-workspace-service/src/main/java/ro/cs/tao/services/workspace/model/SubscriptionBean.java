package ro.cs.tao.services.workspace.model;

import ro.cs.tao.subscription.DataSubscription;

import java.time.LocalDateTime;

public class SubscriptionBean {
    private final DataSubscription subscription;
    private final int subscribers;

    public SubscriptionBean(DataSubscription subscription, int subscribers) {
        this.subscription = subscription;
        this.subscribers = subscribers;
    }

    public Long getId() {
        return subscription.getId();
    }

    public String getName() {
        return subscription.getName();
    }

    public String getDataRootPath() {
        return subscription.getDataRootPath();
    }

    public LocalDateTime getCreated() {
        return subscription.getCreated();
    }

    public int getSubscribers() {
        return subscribers;
    }
}
