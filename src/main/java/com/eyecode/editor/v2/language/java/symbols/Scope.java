package com.eyecode.editor.v2.language.java.symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Scope {

    private final Scope parent;
    private final ProjectSymbol owner;
    private final List<ProjectSymbol> symbols = new ArrayList<>();
    private final List<Scope> children = new ArrayList<>();

    public Scope(Scope parent, ProjectSymbol owner) {
        this.parent = parent;
        this.owner = owner;
    }

    public Scope getParent() {
        return parent;
    }

    public ProjectSymbol getOwner() {
        return owner;
    }

    public List<ProjectSymbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public List<Scope> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addSymbol(ProjectSymbol symbol) {
        if (symbol != null) {
            symbols.add(symbol);
        }
    }

    public Scope addChild(ProjectSymbol owner) {
        Scope child = new Scope(this, owner);
        children.add(child);
        return child;
    }
}
