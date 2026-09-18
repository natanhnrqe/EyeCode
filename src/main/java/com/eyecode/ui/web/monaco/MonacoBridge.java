package com.eyecode.ui.web.monaco;

import java.util.function.Consumer;

public interface MonacoBridge {
    void send(MonacoCommand command);

    default void setEventListener(Consumer<MonacoEvent> listener) { }

    void dispose();
}
