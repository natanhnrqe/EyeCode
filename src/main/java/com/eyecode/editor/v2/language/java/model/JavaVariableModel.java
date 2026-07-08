package com.eyecode.editor.v2.language.java.model;

public final class JavaVariableModel {

    private String name;
    private String type;
    private String ownerMethod;

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
}
