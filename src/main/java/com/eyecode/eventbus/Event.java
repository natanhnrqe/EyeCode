package com.eyecode.eventbus;

/**
 * Marker for an in-process event published through the application-owned
 * {@link EventBus}. Events are immutable facts where practical and are routed
 * only to subscribers of their exact runtime type.
 */
public interface Event {
}
