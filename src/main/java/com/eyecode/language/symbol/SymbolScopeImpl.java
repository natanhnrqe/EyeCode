package com.eyecode.language.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link SymbolScope} (Sprint 5.4a).
 * <p>
 * This implementation uses a simple map for declared symbols and a list
 * for child scopes. It is not thread-safe — synchronization is handled
 * at the {@link ProjectSymbolTable} level.
 */
public final class SymbolScopeImpl implements SymbolScope {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final long id;
    private final ScopeKind kind;
    private final SymbolScope parent;
    private final Map<String, Symbol> declaredSymbols;
    private final List<SymbolScope> children;

    private SymbolScopeImpl(ScopeKind kind, SymbolScope parent) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.parent = parent;
        this.declaredSymbols = new HashMap<>();
        this.children = new ArrayList<>();
    }

    public static SymbolScopeImpl root() {
        return new SymbolScopeImpl(ScopeKind.ROOT, null);
    }

    public static SymbolScopeImpl createChild(SymbolScope parent, ScopeKind kind) {
        SymbolScopeImpl child = new SymbolScopeImpl(kind, parent);
        parent.children().add(child);
        return child;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public ScopeKind kind() {
        return kind;
    }

    @Override
    public Optional<SymbolScope> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<SymbolScope> children() {
        return List.copyOf(children);
    }

    @Override
    public List<Symbol> declaredSymbols() {
        return List.copyOf(declaredSymbols.values());
    }

    public void declare(Symbol symbol) {
        if (declaredSymbols.containsKey(symbol.name())) {
            throw new IllegalStateException("duplicate declaration: " + symbol.name() + " in scope " + this);
        }
        declaredSymbols.put(symbol.name(), symbol);
    }

    @Override
    public Optional<Symbol> findLocal(String name) {
        return Optional.ofNullable(declaredSymbols.get(name));
    }

    @Override
    public Optional<Symbol> lookup(String name) {
        Symbol local = declaredSymbols.get(name);
        if (local != null) {
            return Optional.of(local);
        }
        if (parent != null) {
            return parent.lookup(name);
        }
        return Optional.empty();
    }

    @Override
    public boolean declares(String name) {
        return declaredSymbols.containsKey(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolScopeImpl that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Scope[" + kind + "#" + id + "]";
    }
}