package com.eyecode.editor.v2.language.java.symbols;

import com.eyecode.editor.v2.language.java.model.JavaModifier;

import java.nio.file.Path;
import java.util.EnumSet;

public final class ProjectSymbol {

    private String name;
    private SymbolKind kind;
    private String owner;
    private String type;
    private String signature;
    private EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
    private Path sourceFile;
    private int line = -1;
    private int column = -1;
    private Object astReference;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public void setKind(SymbolKind kind) {
        this.kind = kind;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public EnumSet<JavaModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(EnumSet<JavaModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(Path sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public Object getAstReference() {
        return astReference;
    }

    public void setAstReference(Object astReference) {
        this.astReference = astReference;
    }
}
