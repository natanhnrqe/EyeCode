package com.eyecode.editor.v2.syntax;

import java.util.HashMap;
import java.util.Map;

public final class DocumentStyleRegistry {

    private final Map<String, StyleDefinition> styles = new HashMap<>();

    public void register(String name, StyleDefinition definition) {
        if (name == null || definition == null) return;
        styles.put(name, definition);
    }

    public StyleDefinition get(String name) {
        return styles.get(name);
    }
}
