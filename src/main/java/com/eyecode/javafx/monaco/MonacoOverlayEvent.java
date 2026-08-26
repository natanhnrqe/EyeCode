package com.eyecode.javafx.monaco;

public record MonacoOverlayEvent(
        Type type,
        String overlayId,
        MonacoOverlayAction action,
        long generation,
        String target
) {
    public enum Type {
        POINTER_ENTER,
        POINTER_LEAVE,
        HIDDEN,
        ACTION
    }

    public MonacoOverlayEvent {
        type = type == null ? Type.POINTER_LEAVE : type;
        overlayId = overlayId == null ? "" : overlayId;
        target = target == null ? "" : target;
    }

    public MonacoOverlayEvent(Type type, String overlayId, MonacoOverlayAction action, long generation) {
        this(type, overlayId, action, generation, "");
    }
}
