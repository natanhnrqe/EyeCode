package com.eyecode.editor.v2.language.java.model;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.EnumSet;

public final class JavaFieldModel {

    private String name;
    private String type;
    private EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
    private String owner;
    private TextRange range;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EnumSet<JavaModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(EnumSet<JavaModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Absolute range of the field declaration in the source document
     * (Sprint 5.3a).
     */
    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }
}
