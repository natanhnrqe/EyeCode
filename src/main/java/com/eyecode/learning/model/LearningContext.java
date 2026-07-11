package com.eyecode.learning.model;

import com.eyecode.editor.v2.language.java.symbols.Scope;
import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;

import java.nio.file.Path;

public final class LearningContext {

    private Path currentFile;
    private ProjectSymbol currentClass;
    private ProjectSymbol currentMethod;
    private ProjectSymbol currentSymbol;
    private Object currentNode;
    private Scope currentScope;
    private int cursorOffset;

    public Path getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(Path currentFile) {
        this.currentFile = currentFile;
    }

    public ProjectSymbol getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(ProjectSymbol currentClass) {
        this.currentClass = currentClass;
    }

    public ProjectSymbol getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(ProjectSymbol currentMethod) {
        this.currentMethod = currentMethod;
    }

    public ProjectSymbol getCurrentSymbol() {
        return currentSymbol;
    }

    public void setCurrentSymbol(ProjectSymbol currentSymbol) {
        this.currentSymbol = currentSymbol;
    }

    public Object getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Object currentNode) {
        this.currentNode = currentNode;
    }

    public Scope getCurrentScope() {
        return currentScope;
    }

    public void setCurrentScope(Scope currentScope) {
        this.currentScope = currentScope;
    }

    public int getCursorOffset() {
        return cursorOffset;
    }

    public void setCursorOffset(int cursorOffset) {
        this.cursorOffset = cursorOffset;
    }
}
