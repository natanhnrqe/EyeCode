package com.eyecode.ui.web;

@FunctionalInterface
/**
 * Handles one named Web Shell command. Returning {@code null} suppresses a
 * response; request handlers normally return {@link WebShellEnvelope#response(java.util.Map)}
 * or {@link WebShellEnvelope#error(WebShellError)} derived from the request.
 */
public interface WebShellMessageHandler {
    WebShellEnvelope handle(WebShellEnvelope message);
}
