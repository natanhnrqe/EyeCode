package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe, versioned implementation of {@link SymbolTable} (Sprint 5.4a).
 * <p>
 * This implementation uses a {@link ReadWriteLock} to allow concurrent
 * reads while serializing writes. It maintains a single root scope and
 * all scopes/symbols/references reachable from it.
 * <p>
 * Snapshots are produced by copying the current state under the write lock,
 * ensuring a consistent view for readers.
 */
public final class ProjectSymbolTable implements SymbolTable {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final SymbolScopeImpl rootScope;
    private final Map<SymbolId, Symbol> symbolsById;
    private final Map<Long, SymbolScopeImpl> scopesById;
    private final Map<SymbolId, List<SymbolReference>> referencesByTarget;

    public ProjectSymbolTable() {
        this.symbolsById = new HashMap<>();
        this.scopesById = new HashMap<>();
        this.referencesByTarget = new HashMap<>();
        this.rootScope = SymbolScopeImpl.root();
        registerScopeInternal(rootScope);
    }

    // --- SymbolTable interface ---

    @Override
    public Optional<Symbol> find(SymbolId id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(symbolsById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Symbol> findByName(long scopeId, String name) {
        lock.readLock().lock();
        try {
            SymbolScopeImpl scope = scopesById.get(scopeId);
            if (scope == null) return Optional.empty();
            return scope.findLocal(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Symbol> lookup(long scopeId, String name) {
        lock.readLock().lock();
        try {
            SymbolScopeImpl scope = scopesById.get(scopeId);
            if (scope == null) return Optional.empty();
            return scope.lookup(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Symbol> symbolsIn(long scopeId) {
        lock.readLock().lock();
        try {
            SymbolScopeImpl scope = scopesById.get(scopeId);
            if (scope == null) return List.of();
            return scope.declaredSymbols();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<SymbolScope> scope(long scopeId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(scopesById.get(scopeId));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<SymbolReference> referencesTo(SymbolId symbolId) {
        lock.readLock().lock();
        try {
            List<SymbolReference> refs = referencesByTarget.get(symbolId);
            return refs != null ? List.copyOf(refs) : List.of();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SymbolScope rootScope() {
        lock.readLock().lock();
        try {
            return rootScope;
        } finally {
            lock.readLock().unlock();
        }
    }

    // --- Mutation methods (writer only) ---

    public SymbolScope createChildScope(SymbolScope parent, ScopeKind kind) {
        // Backward-compatible overload that approximates child range as parent's
        // range. Prefer {@link #createChildScope(SymbolScope, ScopeKind, TextRange)}
        // whenever the caller has a more specific range.
        return createChildScope(parent, kind, parent.range());
    }

    public SymbolScope createChildScope(SymbolScope parent, ScopeKind kind, TextRange range) {
        lock.writeLock().lock();
        try {
            SymbolScopeImpl child = SymbolScopeImpl.createChild(parent, kind, range);
            // Register the new scope (and any nested future scopes) in scopesById
            registerScopeInternal(child);
            return child;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void declareSymbol(SymbolScope scope, Symbol symbol) {
        lock.writeLock().lock();
        try {
            if (scope instanceof SymbolScopeImpl impl) {
                impl.declare(symbol);
            } else {
                throw new IllegalArgumentException("scope must be SymbolScopeImpl");
            }
            symbolsById.put(symbol.id(), symbol);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addReference(SymbolReference reference) {
        lock.writeLock().lock();
        try {
            referencesByTarget.computeIfAbsent(reference.target(), k -> new ArrayList<>())
                    .add(reference);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void registerScopeInternal(SymbolScopeImpl scope) {
        scopesById.put(scope.id(), scope);
        for (SymbolScope child : scope.children()) {
            registerScopeInternal((SymbolScopeImpl) child);
        }
    }

    /**
     * Produces an immutable snapshot of the current symbol table state.
     * This method acquires the read lock to ensure a consistent view.
     */
    public SymbolTable snapshotTable(long version, String sourceFile) {
        lock.readLock().lock();
        try {
            return new ProjectSymbolTableSnapshot(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Produces an immutable snapshot of the current state.
     * This method acquires the read lock to ensure a consistent view.
     */
    public SemanticModelSnapshot snapshot(long version, String sourceFile) {
        lock.readLock().lock();
        try {
            // Create a deep copy of the current state
            ProjectSymbolTableSnapshot snapshot = new ProjectSymbolTableSnapshot(this);
            return new SemanticModelSnapshot(version, snapshot, sourceFile);
        } finally {
            lock.readLock().unlock();
        }
    }

    // --- Internal snapshot class ---

    private static final class ProjectSymbolTableSnapshot implements SymbolTable {

        private final Map<SymbolId, Symbol> symbolsById;
        private final Map<Long, SymbolScope> scopesById;
        private final Map<SymbolId, List<SymbolReference>> referencesByTarget;
        private final SymbolScopeImpl rootScope;

        ProjectSymbolTableSnapshot(ProjectSymbolTable source) {
            this.symbolsById = Map.copyOf(source.symbolsById);
            this.referencesByTarget = Map.copyOf(source.referencesByTarget);
            this.scopesById = Map.copyOf(source.scopesById);
            this.rootScope = source.rootScope;
        }

        @Override
        public Optional<Symbol> find(SymbolId id) {
            return Optional.ofNullable(symbolsById.get(id));
        }

        @Override
        public Optional<Symbol> findByName(long scopeId, String name) {
            SymbolScope scope = scopesById.get(scopeId);
            return scope != null ? scope.findLocal(name) : Optional.empty();
        }

        @Override
        public Optional<Symbol> lookup(long scopeId, String name) {
            SymbolScope scope = scopesById.get(scopeId);
            return scope != null ? scope.lookup(name) : Optional.empty();
        }

        @Override
        public List<Symbol> symbolsIn(long scopeId) {
            SymbolScope scope = scopesById.get(scopeId);
            return scope != null ? scope.declaredSymbols() : List.of();
        }

        @Override
        public Optional<SymbolScope> scope(long scopeId) {
            return Optional.ofNullable(scopesById.get(scopeId));
        }

        @Override
        public List<SymbolReference> referencesTo(SymbolId symbolId) {
            List<SymbolReference> refs = referencesByTarget.get(symbolId);
            return refs != null ? List.copyOf(refs) : List.of();
        }

        @Override
        public SymbolScope rootScope() {
            return rootScope;
        }
    }
}