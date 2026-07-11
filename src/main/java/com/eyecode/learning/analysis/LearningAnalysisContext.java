package com.eyecode.learning.analysis;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.Scope;
import com.eyecode.learning.model.LearningContext;

import java.util.Objects;

public final class LearningAnalysisContext {

    private final LearningContext learningContext;
    private final ProjectSymbol currentSymbol;
    private final Scope currentScope;
    private final Object currentNode;
    private final Object currentClass;
    private final Object currentMethod;

    public LearningAnalysisContext(
            LearningContext learningContext,
            ProjectSymbol currentSymbol,
            Scope currentScope,
            Object currentNode,
            Object currentClass,
            Object currentMethod
    ) {
        this.learningContext = learningContext;
        this.currentSymbol = currentSymbol;
        this.currentScope = currentScope;
        this.currentNode = currentNode;
        this.currentClass = currentClass;
        this.currentMethod = currentMethod;
    }

    public LearningContext getLearningContext() {
        return learningContext;
    }

    public ProjectSymbol getCurrentSymbol() {
        return currentSymbol;
    }

    public Scope getCurrentScope() {
        return currentScope;
    }

    public Object getCurrentNode() {
        return currentNode;
    }

    public Object getCurrentClass() {
        return currentClass;
    }

    public Object getCurrentMethod() {
        return currentMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LearningAnalysisContext that)) return false;
        return Objects.equals(learningContext, that.learningContext)
                && Objects.equals(currentSymbol, that.currentSymbol)
                && Objects.equals(currentScope, that.currentScope)
                && Objects.equals(currentNode, that.currentNode)
                && Objects.equals(currentClass, that.currentClass)
                && Objects.equals(currentMethod, that.currentMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(learningContext, currentSymbol, currentScope,
                currentNode, currentClass, currentMethod);
    }
}
