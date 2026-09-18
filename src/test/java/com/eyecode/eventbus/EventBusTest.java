package com.eyecode.eventbus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventBusTest {
    @Test
    void routesOnlyToTheExactRuntimeType() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();
        bus.subscribe(Event.class, ignored -> received.add("event"));
        bus.subscribe(TestEvent.class, ignored -> received.add("test"));

        bus.publish(new TestEvent());

        assertEquals(List.of("test"), received);
    }

    @Test
    void invokesSubscribersInRegistrationOrderAndUnsubscribesByToken() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();
        SubscriptionToken removed = bus.subscribe(TestEvent.class, ignored -> received.add("first"));
        bus.subscribe(TestEvent.class, ignored -> received.add("second"));

        bus.publish(new TestEvent());
        bus.unsubscribe(removed);
        bus.unsubscribe(removed);
        bus.publish(new TestEvent());

        assertEquals(List.of("first", "second", "second"), received);
    }

    @Test
    void propagatesHandlerFailuresAndStopsLaterHandlers() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();
        bus.subscribe(TestEvent.class, ignored -> { throw new IllegalStateException("failure"); });
        bus.subscribe(TestEvent.class, ignored -> received.add("later"));

        assertThrows(IllegalStateException.class, () -> bus.publish(new TestEvent()));

        assertEquals(List.of(), received);
    }

    private record TestEvent() implements Event { }
}
