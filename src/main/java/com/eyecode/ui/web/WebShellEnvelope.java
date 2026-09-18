package com.eyecode.ui.web;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * Versioned Web Shell wire envelope shared by the Java adapter and the React
 * bridge. It is a Web contract, not a Core DTO: payload semantics are owned by
 * the {@code channel}/{@code name} handler pair. Requests receive a response
 * with the same correlation fields; events have no response obligation.
 */
public record WebShellEnvelope(
        String protocol,
        Kind kind,
        String channel,
        String name,
        String requestId,
        String workspaceId,
        String documentId,
        Long documentVersion,
        Map<String, Object> payload,
        WebShellError error
) {
    public static final String PROTOCOL = "eyecode.web/1";

    public enum Kind { REQUEST, RESPONSE, EVENT }

    public WebShellEnvelope {
        protocol = protocol == null ? PROTOCOL : protocol;
        kind = kind == null ? Kind.EVENT : kind;
        channel = channel == null ? "" : channel;
        name = name == null ? "" : name;
        requestId = requestId == null ? "" : requestId;
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static WebShellEnvelope request(String channel, String name, String requestId,
                                           Map<String, Object> payload) {
        return new WebShellEnvelope(PROTOCOL, Kind.REQUEST, channel, name, requestId,
                null, null, null, payload, null);
    }

    public static WebShellEnvelope event(String channel, String name, Map<String, Object> payload) {
        return new WebShellEnvelope(PROTOCOL, Kind.EVENT, channel, name, "",
                null, null, null, payload, null);
    }

    public WebShellEnvelope response(Map<String, Object> responsePayload) {
        return new WebShellEnvelope(PROTOCOL, Kind.RESPONSE, channel, name, requestId,
                workspaceId, documentId, documentVersion, responsePayload, null);
    }

    public WebShellEnvelope error(WebShellError responseError) {
        return new WebShellEnvelope(PROTOCOL, Kind.RESPONSE, channel, name, requestId,
                workspaceId, documentId, documentVersion, Map.of(), responseError);
    }
}
