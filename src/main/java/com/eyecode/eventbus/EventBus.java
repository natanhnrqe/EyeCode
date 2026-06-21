package com.eyecode.eventbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Application-scoped event bus for IDE communication.
 * <p>
 * Subscribers receive events only for the exact subscribed type.
 * Event inheritance dispatch is NOT supported.
 * This is intentional for simplicity and predictability.
 */
public class EventBus {

    private final Map<Class<?>, CopyOnWriteArrayList<SubscriberEntry<?>>> subscribersByType;
    private final Map<SubscriptionToken, SubscriberEntry<?>> tokenIndex;

    public EventBus() {
        this.subscribersByType = new ConcurrentHashMap<>();
        this.tokenIndex = new ConcurrentHashMap<>();
    }

    public <T> SubscriptionToken subscribe(Class<T> eventType, Consumer<T> handler) {
        SubscriptionToken token = new SubscriptionToken();
        SubscriberEntry<T> entry = new SubscriberEntry<>(token, eventType, handler);
        subscribersByType
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(entry);
        tokenIndex.put(token, entry);
        return token;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        Class<?> eventType = event.getClass();
        CopyOnWriteArrayList<SubscriberEntry<?>> subscribers = subscribersByType.get(eventType);
        if (subscribers == null) return;
        for (SubscriberEntry<?> entry : subscribers) {
            ((Consumer<T>) entry.handler).accept(event);
        }
    }

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
