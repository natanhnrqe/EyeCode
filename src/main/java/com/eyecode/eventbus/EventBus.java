package com.eyecode.eventbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Application-scoped, synchronous event bus for IDE communication.
 * <p>
 * A composition root creates the bus and passes that instance to participating
 * application services. Subscribers receive events only for the exact runtime
 * type; event inheritance dispatch is intentionally unsupported. Publication
 * invokes handlers in subscription order on the caller's thread. A handler
 * exception propagates to the publisher and prevents later handlers from
 * running, so publishers must choose an execution boundary appropriate to
 * their caller.
 * <p>
 * Subscriptions remain registered until {@link #unsubscribe(SubscriptionToken)}
 * is called. Long-lived owners must release their tokens during disposal.
 */
public class EventBus {

    private final Map<Class<?>, CopyOnWriteArrayList<SubscriberEntry<?>>> subscribersByType;
    private final Map<SubscriptionToken, SubscriberEntry<?>> tokenIndex;

    public EventBus() {
        this.subscribersByType = new ConcurrentHashMap<>();
        this.tokenIndex = new ConcurrentHashMap<>();
    }

    /**
     * Registers a handler for one exact event type.
     *
     * @return a token owned by the subscriber and required for unregistration
     */
    public <T> SubscriptionToken subscribe(Class<T> eventType, Consumer<T> handler) {
        SubscriptionToken token = new SubscriptionToken();
        SubscriberEntry<T> entry = new SubscriberEntry<>(token, eventType, handler);
        subscribersByType
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(entry);
        tokenIndex.put(token, entry);
        return token;
    }

    /**
     * Delivers an event synchronously to the current snapshot of subscribers.
     * No scheduling, error isolation, or polymorphic routing is applied.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        Class<?> eventType = event.getClass();
        CopyOnWriteArrayList<SubscriberEntry<?>> subscribers = subscribersByType.get(eventType);
        if (subscribers == null) return;
        for (SubscriberEntry<?> entry : subscribers) {
            ((Consumer<T>) entry.handler).accept(event);
        }
    }

    /**
     * Removes the subscription identified by {@code token}. Unknown tokens are
     * harmless, which makes repeated lifecycle cleanup safe.
     */
    public void unsubscribe(SubscriptionToken token) {
        SubscriberEntry<?> entry = tokenIndex.remove(token);
        if (entry == null) return;
        CopyOnWriteArrayList<SubscriberEntry<?>> subscribers = subscribersByType.get(entry.eventType);
        if (subscribers != null) {
            subscribers.remove(entry);
        }
    }

    private record SubscriberEntry<T>(SubscriptionToken token, Class<?> eventType, Consumer<T> handler) { }
}
