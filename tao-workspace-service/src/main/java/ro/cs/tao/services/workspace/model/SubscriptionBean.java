package ro.cs.tao.services.workspace.model;

import ro.cs.tao.subscription.DataSubscription;

import java.time.LocalDateTime;

public class SubscriptionBean {
    private final DataSubscription subscription;

    public SubscriptionBean(DataSubscription subscription) {
        this.subscription = subscription;
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

    public String getWorkspaceId() {
        return subscription.getRepositoryId();
    }

    public int getSubscribers() {
        return subscription.getSubscribersCount();
    }

    public LocalDateTime getCreated() {
        return subscription.getCreated();
    }

}
