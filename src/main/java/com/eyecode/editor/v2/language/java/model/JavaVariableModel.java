package com.eyecode.editor.v2.language.java.model;

import com.eyecode.editor.intelligence.document.TextRange;

public final class JavaVariableModel {

    private String name;
    private String type;
    private String ownerMethod;
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

    public String getOwnerMethod() {
        return ownerMethod;
    }

    public void setOwnerMethod(String ownerMethod) {
        this.ownerMethod = ownerMethod;
    }

    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }
}
