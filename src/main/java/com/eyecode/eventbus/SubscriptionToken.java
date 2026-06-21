package com.eyecode.eventbus;

import java.util.Objects;
import java.util.UUID;

public class SubscriptionToken {

    private final UUID id;

    public SubscriptionToken() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionToken that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
