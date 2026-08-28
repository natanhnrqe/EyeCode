package com.eyecode.javafx.web;

public record WebShellError(String code, String message, boolean recoverable) {
    public WebShellError {
        code = code == null ? "UNKNOWN_ERROR" : code;
        message = message == null ? "Unknown Web Shell error" : message;
    }
}
