package com.eyecode.javafx.ui.toolwindow.content;

final class LearningHtmlPayload {

    private LearningHtmlPayload() {
    }

    static String updateScript(String html) {
        String payload = object("html", html == null ? "" : html);
        return "window.eyeCodeLearningUpdate(JSON.parse(" + string(payload) + "))";
    }

    private static String object(String key, String value) {
        return "{" + string(key) + ":" + string(value) + "}";
    }

    private static String string(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20 || character > 0x7e) {
                        result.append("\\u").append(String.format("%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
