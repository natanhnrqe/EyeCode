package com.eyecode.editor.v2.language.java.model;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class JavaClassModel {

    private String name;
    private TypeKind kind;
    private EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
    private String superClass;
    private List<String> interfaces = new ArrayList<>();
    private List<JavaFieldModel> fields = new ArrayList<>();
    private List<JavaMethodModel> methods = new ArrayList<>();
    private List<JavaConstructorModel> constructors = new ArrayList<>();
    private List<JavaClassModel> nestedTypes = new ArrayList<>();
    private TextRange range;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeKind getKind() {
        return kind;
    }

    public void setKind(TypeKind kind) {
        this.kind = kind;
    }

    public EnumSet<JavaModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(EnumSet<JavaModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public List<JavaFieldModel> getFields() {
        return fields;
    }

    public void setFields(List<JavaFieldModel> fields) {
        this.fields = fields;
    }

    public List<JavaMethodModel> getMethods() {
        return methods;
    }

    public void setMethods(List<JavaMethodModel> methods) {
        this.methods = methods;
    }

    public List<JavaConstructorModel> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<JavaConstructorModel> constructors) {
        this.constructors = constructors;
    }

    public List<JavaClassModel> getNestedTypes() {
        return nestedTypes;
    }

    public void setNestedTypes(List<JavaClassModel> nestedTypes) {
        this.nestedTypes = nestedTypes;
    }

    /**
     * Absolute range of the type declaration in the source document
     * (Sprint 5.3a).
     */
    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }
}
