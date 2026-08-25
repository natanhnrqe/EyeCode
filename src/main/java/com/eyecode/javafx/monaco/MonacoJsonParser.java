package com.eyecode.javafx.monaco;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MonacoJsonParser {
    private final String input;
    private int index;

    private MonacoJsonParser(String input) {
        this.input = input == null ? "" : input;
    }

    static Map<String, Object> parseObject(String input) {
        MonacoJsonParser parser = new MonacoJsonParser(input);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!(value instanceof Map<?, ?> map) || parser.index != parser.input.length()) {
            throw new IllegalArgumentException("Invalid Monaco JSON object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= input.length()) throw error();
        return switch (input.charAt(index)) {
            case '{' -> parseObjectValue();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseLiteral("true", Boolean.TRUE);
            case 'f' -> parseLiteral("false", Boolean.FALSE);
            case 'n' -> parseLiteral("null", null);
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObjectValue() {
        Map<String, Object> result = new LinkedHashMap<>();
        index++;
        skipWhitespace();
        if (consume('}')) return result;
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            require(':');
            result.put(key, parseValue());
            skipWhitespace();
            if (consume('}')) return result;
            require(',');
        }
    }

    private List<Object> parseArray() {
        List<Object> result = new ArrayList<>();
        index++;
        skipWhitespace();
        if (consume(']')) return result;
        while (true) {
            result.add(parseValue());
            skipWhitespace();
            if (consume(']')) return result;
            require(',');
        }
    }

    private String parseString() {
        require('"');
        StringBuilder result = new StringBuilder();
        while (index < input.length()) {
            char current = input.charAt(index++);
            if (current == '"') return result.toString();
            if (current != '\\') {
                result.append(current);
                continue;
            }
            if (index >= input.length()) throw error();
            char escaped = input.charAt(index++);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (index + 4 > input.length()) throw error();
                    result.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
                }
                default -> throw error();
            }
            if (escaped == 'u') index += 4;
        }
        throw error();
    }

    private Object parseLiteral(String literal, Object value) {
        if (!input.startsWith(literal, index)) throw error();
        index += literal.length();
        return value;
    }

    private Number parseNumber() {
        int start = index;
        while (index < input.length() && "-+0123456789.eE".indexOf(input.charAt(index)) >= 0) index++;
        String value = input.substring(start, index);
        try {
            return value.contains(".") || value.contains("e") || value.contains("E")
                    ? Double.parseDouble(value) : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw error();
        }
    }

    private void skipWhitespace() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
    }

    private boolean consume(char expected) {
        if (index < input.length() && input.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void require(char expected) {
        if (!consume(expected)) throw error();
    }

    private IllegalArgumentException error() {
        return new IllegalArgumentException("Invalid Monaco JSON at index " + index);
    }
}
