package com.eyecode.editor.v2.language.java.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class JavaMethodModel {

    private String name;
    private String returnType;
    private EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
    private List<JavaParameterModel> parameters = new ArrayList<>();
    private List<JavaVariableModel> localVariables = new ArrayList<>();
    private String owner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public EnumSet<JavaModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(EnumSet<JavaModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<JavaParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(List<JavaParameterModel> parameters) {
        this.parameters = parameters;
    }

    public List<JavaVariableModel> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<JavaVariableModel> localVariables) {
        this.localVariables = localVariables;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
