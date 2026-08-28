package com.eyecode.javafx.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellProtocolCodec {
    public WebShellEnvelope decode(String json) {
        Object value = new Parser(json).value();
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Web Shell envelope must be an object");
        }
        String protocol = text(raw.get("protocol"));
        if (!WebShellEnvelope.PROTOCOL.equals(protocol)) {
            throw new IllegalArgumentException("Unsupported Web Shell protocol: " + protocol);
        }
        WebShellEnvelope.Kind kind;
        try {
            kind = WebShellEnvelope.Kind.valueOf(text(raw.get("kind")).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Web Shell message kind", exception);
        }
        Long version = raw.get("documentVersion") instanceof Number number ? number.longValue() : null;
        WebShellError error = null;
        if (raw.get("error") instanceof Map<?, ?> errorMap) {
            error = new WebShellError(text(errorMap.get("code")), text(errorMap.get("message")),
                    Boolean.TRUE.equals(errorMap.get("recoverable")));
        }
        Map<String, Object> payload = raw.get("payload") instanceof Map<?, ?> map
                ? stringMap(map) : Map.of();
        return new WebShellEnvelope(protocol, kind, text(raw.get("channel")), text(raw.get("name")),
                text(raw.get("requestId")), nullableText(raw.get("workspaceId")),
                nullableText(raw.get("documentId")), version, payload, error);
    }

    public String encode(WebShellEnvelope message) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", message.protocol());
        values.put("kind", message.kind().name().toLowerCase(java.util.Locale.ROOT));
        values.put("channel", message.channel());
        values.put("name", message.name());
        values.put("requestId", message.requestId());
        values.put("workspaceId", message.workspaceId());
        values.put("documentId", message.documentId());
        values.put("documentVersion", message.documentVersion());
        values.put("payload", message.payload());
        if (message.error() != null) {
            values.put("error", Map.of("code", message.error().code(), "message", message.error().message(),
                    "recoverable", message.error().recoverable()));
        }
        return json(values);
    }

    private static Map<String, Object> stringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String nullableText(Object value) { return value == null ? null : String.valueOf(value); }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return '"' + escape(text) + '"';
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(json(String.valueOf(entry.getKey()))).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder result = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) result.append(',');
                first = false;
                result.append(json(item));
            }
            return result.append(']').toString();
        }
        return json(String.valueOf(value));
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> result.append(c);
            }
        }
        return result.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;
        private Parser(String input) { this.input = input == null ? "" : input; }
        private Object value() {
            whitespace();
            if (index >= input.length()) throw error();
            Object result = switch (input.charAt(index)) {
                case '{' -> object(); case '[' -> array(); case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE); case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null); default -> number();
            };
            whitespace();
            if (index != input.length()) throw error();
            return result;
        }
        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>(); index++;
            whitespace(); if (consume('}')) return result;
            while (true) { whitespace(); String key = string(); whitespace(); require(':'); result.put(key, nested());
                whitespace(); if (consume('}')) return result; require(','); }
        }
        private List<Object> array() {
            List<Object> result = new ArrayList<>(); index++; whitespace(); if (consume(']')) return result;
            while (true) { result.add(nested()); whitespace(); if (consume(']')) return result; require(','); }
        }
        private Object nested() {
            whitespace(); if (index >= input.length()) throw error();
            return switch (input.charAt(index)) { case '{' -> object(); case '[' -> array(); case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE); case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null); default -> number(); };
        }
        private String string() { require('"'); StringBuilder result = new StringBuilder();
            while (index < input.length()) { char c = input.charAt(index++); if (c == '"') return result.toString();
                if (c != '\\') { result.append(c); continue; } if (index >= input.length()) throw error(); char e = input.charAt(index++);
                switch (e) { case '"', '\\', '/' -> result.append(e); case 'b' -> result.append('\b'); case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n'); case 'r' -> result.append('\r'); case 't' -> result.append('\t');
                    case 'u' -> { if (index + 4 > input.length()) throw error(); result.append((char) Integer.parseInt(input.substring(index, index + 4), 16)); index += 4; }
                    default -> throw error(); } } throw error(); }
        private Object literal(String literal, Object result) { if (!input.startsWith(literal, index)) throw error(); index += literal.length(); return result; }
        private Number number() { int start = index; while (index < input.length() && "-+0123456789.eE".indexOf(input.charAt(index)) >= 0) index++;
            String value = input.substring(start, index); try { return value.contains(".") || value.contains("e") || value.contains("E") ? Double.parseDouble(value) : Long.parseLong(value); }
            catch (NumberFormatException exception) { throw error(); } }
        private void whitespace() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
        private boolean consume(char c) { if (index < input.length() && input.charAt(index) == c) { index++; return true; } return false; }
        private void require(char c) { if (!consume(c)) throw error(); }
        private IllegalArgumentException error() { return new IllegalArgumentException("Invalid Web Shell JSON at index " + index); }
    }
}
