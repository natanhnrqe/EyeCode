package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.ScopeKind;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Java implementation of {@link NameResolver} (Sprint 5.4b.1, rewritten in 5.4b.2).
 * <p>
 * Simple-name resolution is performed by a single primary entry point:
 * {@link #resolve(SymbolReference, SymbolTable)}. It uses
 * {@link SymbolTable#lookup(long, String)} which walks the
 * hierarchical scope chain from the reference's scope toward the root,
 * returning the innermost declaration for a simple name. This naturally
 * implements Java shadowing — a declaration in the innermost scope hides
 * any same-named declaration in an outer scope.
 * <p>
 * The batch entry point {@link #resolve(ParserSnapshot, SymbolTable)}
 * walks the AST pre-order, builds a {@link SymbolReference}
 * (name + scopeId + range) for each simple-name occurrence, and delegates
 * to the single-reference entry point so both callers share the exact
 * same lookup / shadowing semantics.
 * <p>
 * What it resolves (this sprint):
 * <ul>
 *   <li>local variables ({@link SymbolKind#LOCAL_VARIABLE});</li>
 *   <li>method/constructor parameters ({@link SymbolKind#PARAMETER});</li>
 *   <li>fields ({@link SymbolKind#FIELD});</li>
 *   <li>types — class/interface/enum/record ({@link SymbolKind#TYPE},
 *       {@link SymbolKind#INTERFACE}, {@link SymbolKind#ENUM}) —
 *       when a symbol exists in an enclosing scope;</li>
 *   <li>type parameters ({@link SymbolKind#TYPE_PARAMETER});</li>
 *   <li>methods referenced unqualified ({@code run()} where {@code run}
 *       is a symbol in the enclosing TYPE scope) — overload matching
 *       and signature discrimination are NOT performed;</li>
 *   <li>constructors referenced by the simple type name — also unqualified.</li>
 * </ul>
 * <p>
 * Out of scope (deferred to 5.4b.3 and later):
 * overload resolution, method signature matching, inheritance, interface
 * implementation, generic substitution, type inference, qualified names
 * ({@code a.b.C}), field chains, static imports, regular imports,
 * stdlib / cross-file resolution, type-name resolution in typed
 * declarations, JLS-faithful shadowing in all corner cases, ambiguity
 * detection (this model is lexically innermost-wins so it never
 * produces {@link ResolutionKind#AMBIGUOUS}).
 * <p>
 * The resolver never throws on unresolved names — it produces
 * {@link ResolvedSymbolReference#unresolved(SymbolReference)} results.
 * The AST, the {@link SymbolTable}, the {@link SymbolScope} and the
 * {@link SymbolReference} input are never mutated. Results are returned
 * as an immutable list (defensive copy).
 * <p>
 * Pure Core: zero Swing / JavaFX / AWT / editor-ui / workbench dependencies.
 */
public final class JavaNameResolver implements NameResolver {

    @Override
    public ResolvedSymbolReference resolve(SymbolReference reference, SymbolTable symbolTable) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(symbolTable, "symbolTable must not be null");

        String name = reference.name();
        long scopeId = reference.scopeId();
        Optional<Symbol> found = symbolTable.lookup(scopeId, name);
        if (found.isPresent()) {
            SymbolId id = found.get().id();
            return ResolvedSymbolReference.resolved(reference, id);
        }
        return ResolvedSymbolReference.unresolved(reference);
    }

    @Override
    public List<ResolvedSymbolReference> resolve(ParserSnapshot parserSnapshot, SymbolTable symbolTable) {
        Objects.requireNonNull(parserSnapshot, "parserSnapshot must not be null");
        Objects.requireNonNull(symbolTable, "symbolTable must not be null");

        AstNode astRoot = parserSnapshot.astRoot();
        if (astRoot == null || astRoot.kind() != AstNodeKind.COMPILATION_UNIT) {
            return List.of();
        }

        BatchVisitor visitor = new BatchVisitor(symbolTable);
        visitor.walk(astRoot);
        return visitor.results();
    }

    // ----------------------------------------------------------------------
    // Batch traversal — builds a SymbolReference per NAME_EXPRESSION and
    // delegates to the single-reference entry point.
    // ----------------------------------------------------------------------

    private final class BatchVisitor {

        private final SymbolTable symbolTable;
        private final Deque<SymbolScope> scopeStack = new ArrayDeque<>();
        private final List<ResolvedSymbolReference> results = new ArrayList<>();

        BatchVisitor(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            this.scopeStack.push(symbolTable.rootScope());
        }

        List<ResolvedSymbolReference> results() {
            return List.copyOf(results);
        }

        void walk(AstNode node) {
            switch (node.kind()) {
                case COMPILATION_UNIT -> visitChildren(node);
                case PACKAGE_DECLARATION -> {
                    pushMatching(node, ScopeKind.PACKAGE);
                    visitChildren(node);
                    pop();
                }
                case CLASS_DECLARATION, INTERFACE_DECLARATION, ENUM_DECLARATION, RECORD_DECLARATION -> {
                    pushMatching(node, ScopeKind.TYPE);
                    visitChildren(node);
                    pop();
                }
                case METHOD_DECLARATION, CONSTRUCTOR_DECLARATION -> {
                    pushMatching(node, ScopeKind.METHOD);
                    visitChildren(node);
                    pop();
                }
                case BLOCK -> {
                    pushMatching(node, ScopeKind.BLOCK, true);
                    visitChildren(node);
                    pop();
                }
                case NAME_EXPRESSION -> resolveSimpleName(node);
                case METHOD_CALL_EXPRESSION -> visitMethodCall(node);
                case FIELD_ACCESS_EXPRESSION -> visitFieldAccess(node);
                default -> visitChildren(node);
            }
        }

        private void visitChildren(AstNode node) {
            for (AstNode child : node.children()) {
                walk(child);
            }
        }

        // ---- scope stack management -----------------------------------

        private void pushMatching(AstNode node, ScopeKind target) {
            pushMatching(node, target, false);
        }

        private void pushMatching(AstNode node, ScopeKind target, boolean preferSmallest) {
            SymbolScope current = scopeStack.peek();
            if (current == null) {
                return;
            }
            SymbolScope best = null;
            for (SymbolScope child : current.children()) {
                if (child.kind() != target) {
                    continue;
                }
                if (rangeContains(child.range(), node.range())) {
                    if (best == null || area(child.range()) < area(best.range())) {
                        best = child;
                    }
                }
            }
            scopeStack.push(best != null ? best : current);
        }

        private void pop() {
            if (scopeStack.size() > 1) {
                scopeStack.pop();
            }
        }

        // ---- resolutions ----------------------------------------------

        private void resolveSimpleName(AstNode node) {
            String name = nameOf(node);
            if (name == null || name.isEmpty()) {
                return;
            }
            resolveAndAdd(node, name);
        }

        private void visitMethodCall(AstNode node) {
            List<AstNode> children = node.children();
            if (!children.isEmpty() && children.get(0).kind() == AstNodeKind.NAME_EXPRESSION) {
                resolveSimpleName(children.get(0));
            }
            for (int i = 1; i < children.size(); i++) {
                walk(children.get(i));
            }
        }

        private void visitFieldAccess(AstNode node) {
            List<AstNode> children = node.children();
            for (AstNode child : children) {
                walk(child);
            }
        }

        private void resolveAndAdd(AstNode node, String name) {
            SymbolScope scope = scopeStack.peek();
            long scopeId = scope != null ? scope.id() : symbolTable.rootScope().id();
            SymbolReference reference = SymbolReference.simple(name, scopeId, node.range());
            results.add(JavaNameResolver.this.resolve(reference, symbolTable));
        }

        // ---- utilities ------------------------------------------------

        private static String nameOf(AstNode node) {
            if (node.token() != null) {
                String text = node.token().text();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
            return null;
        }

        private static boolean rangeContains(TextRange outer, TextRange inner) {
            return outer.startOffset() <= inner.startOffset()
                    && inner.endOffset() <= outer.endOffset();
        }

        private static int area(TextRange r) {
            int size = r.endOffset() - r.startOffset();
            return Math.max(size, 0);
        }
    }
}
