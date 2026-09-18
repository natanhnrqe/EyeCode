package com.eyecode.ui.web;

/**
 * Transport-neutral boundary between Web Shell controllers and a rendered Web
 * client. JavaFX/CEFFX, Swing/JCEF, and the loopback development transport
 * implement this interface while controllers only send envelopes and register
 * channel handlers.
 */
public interface WebShellSurface {
    void send(WebShellEnvelope message);

    void registerHandler(String channel, String name, WebShellMessageHandler handler);
}
