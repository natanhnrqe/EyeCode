package com.eyecode.editor.v2.language.java.model;

import com.eyecode.editor.intelligence.document.TextRange;

public final class JavaParameterModel {

    private String name;
    private String type;
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

    /**
     * Absolute range of the parameter (type through name) in the source
     * document (Sprint 5.3a).
     */
    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }
}
