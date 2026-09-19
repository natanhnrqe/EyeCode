package com.eyecode.ui.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WebShellPayload {
    private WebShellPayload() {}

    static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    static long number(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    static List<String> paths(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (!(value instanceof Iterable<?> values)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }
}
