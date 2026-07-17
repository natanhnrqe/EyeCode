package com.eyecode.learning.markdown;

public final class Segment {

    public enum Type { TEXT, BOLD, ITALIC, CODE, LINK }

    private final Type type;
    private final String text;
    private final String url;

    private Segment(Type type, String text, String url) {
        this.type = type;
        this.text = text;
        this.url = url;
    }

    public static Segment text(String text) {
        return new Segment(Type.TEXT, text, null);
    }

    public static Segment bold(String text) {
        return new Segment(Type.BOLD, text, null);
    }

    public static Segment italic(String text) {
        return new Segment(Type.ITALIC, text, null);
    }

    public static Segment code(String text) {
        return new Segment(Type.CODE, text, null);
    }

    public static Segment link(String text, String url) {
        return new Segment(Type.LINK, text, url);
    }

    public Type type() {
        return type;
    }

    public String text() {
        return text;
    }

    public String url() {
        return url;
    }

    @Override
    public String toString() {
        return switch (type) {
            case TEXT -> text;
            case BOLD -> "**" + text + "**";
            case ITALIC -> "*" + text + "*";
            case CODE -> "`" + text + "`";
            case LINK -> "[" + text + "](" + url + ")";
        };
    }
}
