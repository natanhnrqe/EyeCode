package com.eyecode.language.symbol;

import java.util.Objects;

/**
 * Immutable, versioned snapshot of the semantic model (Sprint 5.4a).
 * <p>
 * A snapshot captures the complete state of the symbol table at a specific
 * document/project version. It is immutable and never changes after
 * creation — new versions produce new snapshots.
 */
public final class SemanticModelSnapshot {

    private final long version;
    private final SymbolTable symbolTable;
    private final String sourceFile;

    public SemanticModelSnapshot(long version, SymbolTable symbolTable, String sourceFile) {
        this.version = version;
        this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable must not be null");
        this.sourceFile = sourceFile;
    }

    public long version() {
        return version;
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }

    public String sourceFile() {
        return sourceFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticModelSnapshot that)) return false;
        return version == that.version
                && Objects.equals(symbolTable, that.symbolTable)
                && Objects.equals(sourceFile, that.sourceFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, symbolTable, sourceFile);
    }

    @Override
    public String toString() {
        return "SemanticModelSnapshot{v=" + version + ", file=" + sourceFile + "}";
    }
}