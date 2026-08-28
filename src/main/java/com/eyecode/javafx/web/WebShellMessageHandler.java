package com.eyecode.javafx.web;

@FunctionalInterface
public interface WebShellMessageHandler {
    WebShellEnvelope handle(WebShellEnvelope message);
}
