package com.eyecode.diagnostics;

public record JavaDiagnosticRequest(String uri, String requestId, long modelVersion, String source) {
    public JavaDiagnosticRequest {
        uri = uri == null ? "" : uri;
        requestId = requestId == null ? "" : requestId;
        source = source == null ? "" : source;
    }
}