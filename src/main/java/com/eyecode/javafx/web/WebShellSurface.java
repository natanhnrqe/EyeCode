package com.eyecode.javafx.web;

public interface WebShellSurface {
    void send(WebShellEnvelope message);

    void registerHandler(String channel, String name, WebShellMessageHandler handler);
}
