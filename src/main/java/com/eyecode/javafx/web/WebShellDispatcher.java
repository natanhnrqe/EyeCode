package com.eyecode.javafx.web;

import java.util.HashMap;
import java.util.Map;

public final class WebShellDispatcher {
    private final Map<String, WebShellMessageHandler> handlers = new HashMap<>();

    public void register(String channel, String name, WebShellMessageHandler handler) {
        if (channel == null || name == null || handler == null) {
            throw new IllegalArgumentException("Web Shell handlers require channel, name, and handler");
        }
        handlers.put(key(channel, name), handler);
    }

    public WebShellEnvelope dispatch(WebShellEnvelope message) {
        if (message == null) {
            return null;
        }
        WebShellMessageHandler handler = handlers.get(key(message.channel(), message.name()));
        if (handler == null) {
            return message.kind() == WebShellEnvelope.Kind.REQUEST
                    ? message.error(new WebShellError("UNKNOWN_COMMAND",
                    "Unknown Web Shell command: " + message.channel() + "/" + message.name(), true))
                    : null;
        }
        System.out.println("JAVA DISPATCH matched handler=" + message.channel() + "/"
                + message.name());
        return handler.handle(message);
    }

    private static String key(String channel, String name) {
        return channel + "/" + name;
    }
}
