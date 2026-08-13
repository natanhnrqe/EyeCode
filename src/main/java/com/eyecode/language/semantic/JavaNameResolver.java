package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.ScopeKind;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Java-specific {@link NameResolver} (Sprint 5.4b.1).
 * <p>
 * Resolves simple-name references in Java source by traversing the AST and
 * tracking the current lexical {@link SymbolScope}. The lookup is delegated to
 * {@link SymbolScope#lookup(String)} which walks the hierarchical scope chain
 * {@code BLOCK -> METHOD -> TYPE -> PACKAGE -> ROOT}, returning the innermost
 * declaration for a simple name. This naturally implements Java shadowing:
 * a declaration in the innermost scope hides any same-named declaration in an
 * outer scope.
 * <p>
 * What it resolves:
 * <ul>
 *   <li>local variables inside the body of a method/constructor;</li>
 *   <li>method/constructor parameters;</li>
 *   <li>fields of the enclosing type (looked up via TYPE scope);</li>
 *   <li>method names referenced unqualified ({@code run()} where {@code run}
 *       is a symbol in the enclosing TYPE scope) — overload matching and
 *       signature discrimination are NOT performed;</li>
 *   <li>type names (class/interface/enum/record) when a symbol exists in an
 *       enclosing scope.</li>
 * </ul>
 * <p>
 * Out of scope (deferred to 5.4b.2 and later):
 * overload resolution, method signature matching, inheritance, interface
 * implementation, generic substitution, type inference, qualified names
 * ({@code a.b.C}), field chains, static imports, regular imports, stdlib /
 * cross-file resolution.
 * <p>
 * The resolver never throws on unresolved names — it produces
 * {@link ResolvedSymbolReference#unresolved(SymbolReference)} results with a
 * placeholder {@link SymbolId} (the reference's own range). The real
 * declaration symbol id is never fabricated for resolved entries.
 * <p>
 * Pure Core: zero Swing / JavaFX / AWT / editor-ui / workbench dependencies.
 * The AST and the {@link SymbolTable} are never mutated.
 * Results are returned as an immutable list (defensive copy).
 */
public final class JavaNameResolver implements NameResolver {

    @Override
    public List<ResolvedSymbolReference> resolve(ParserSnapshot parserSnapshot, SymbolTable symbolTable) {
        Objects.requireNonNull(parserSnapshot, "parserSnapshot must not be null");
        Objects.requireNonNull(symbolTable, "symbolTable must not be null");

        AstNode astRoot = parserSnapshot.astRoot();
        if (astRoot == null || astRoot.kind() != AstNodeKind.COMPILATION_UNIT) {
            return List.of();
        }

        ResolverVisitor visitor = new ResolverVisitor(symbolTable);
        visitor.walk(astRoot);
        return visitor.results();
    }

    // ----------------------------------------------------------------------
    // Traversal + resolution
    // ----------------------------------------------------------------------

    /**
     * Recursive visitor that walks the AST pre-order and tracks the current
     * {@link SymbolScope}. Scope push/pop is paired around the recursive
     * descent into scope-creating nodes (CU, package, type, method/constructor,
     * block) so the same visitor is used for every name within a scope and the
     * scope is released exactly once the subtree is exhausted.
     */
    private static final class ResolverVisitor {

        private final SymbolTable symbolTable;
        private final Deque<SymbolScope> scopeStack = new ArrayDeque<>();
        private final List<ResolvedSymbolReference> results = new ArrayList<>();

        ResolverVisitor(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            this.scopeStack.push(symbolTable.rootScope());
        }

        List<ResolvedSymbolReference> results() {
            return List.copyOf(results);
        }

        // ---- traversal entry ------------------------------------------

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

        /**
         * Locate a child scope of the current scope whose kind matches
         * {@code target} and whose range contains the AST node's range.
         * When {@code preferSmallest} is set (used for BLOCK scopes),
         * the tightest containing candidate wins, so nested sibling blocks
         * descend into the innermost one. If no match is found a no-op
         * re-push of the current scope is performed so the pop is always
         * balanced.
         */
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
                    if (best == null) {
                        best = child;
                    } else {
                        int aSize = area(child.range());
                        int bSize = area(best.range());
                        if (preferSmallest ? aSize < bSize : aSize < bSize) {
                            // Prefer the tightest match in both cases — the
                            // SymbolTableBuilder creates sibling TYPE/METHOD
                            // scopes with parent-proxy ranges, so the actual
                            // nested descendant is the only one that tightly
                            // contains the AST node's range.
                            best = child;
                        }
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
                // The parser always attaches a Token to NAME_EXPRESSION leaves,
                // so this should never occur in practice; bail defensively.
                return;
            }
            resolveAndAdd(node, name);
        }

        private void visitMethodCall(AstNode node) {
            // Children of METHOD_CALL_EXPRESSION: [receiver?, args...]
            // receiver may be a NAME_EXPRESSION for an unqualified call `name()`.
            // For any other shape (`this.x()`, `obj.foo()`, `a.b.c()`) we fall
            // through and rely on visitChildren() — every nested NAME_EXPRESSION
            // will be visited and resolved as an ordinary simple name.
            List<AstNode> children = node.children();
            if (!children.isEmpty() && children.get(0).kind() == AstNodeKind.NAME_EXPRESSION) {
                resolveSimpleName(children.get(0));
            }
            // Visit the rest of the children (call args).
            for (int i = 1; i < children.size(); i++) {
                walk(children.get(i));
            }
        }

        private void visitFieldAccess(AstNode node) {
            // `a.b`: children.get(0) is the receiver; we visit it as a simple
            // name reference if applicable. `b` (the accessed field name) is
            // not resolved in this sprint — that needs the static type of the
            // receiver which is outside the scope of 5.4b.1.
            List<AstNode> children = node.children();
            for (int i = 0; i < children.size(); i++) {
                AstNode child = children.get(i);
                // The receiver is already visited as a NAME_EXPRESSION if it
                // is itself a NAME_EXPRESSION; deeper chains are delegated.
                if (i == 0 && child.kind() == AstNodeKind.NAME_EXPRESSION) {
                    walk(child); // walk -> NAME_EXPRESSION -> resolveSimpleName
                } else {
                    walk(child);
                }
            }
        }

        private void resolveAndAdd(AstNode node, String name) {
            SymbolScope scope = scopeStack.peek();
            if (scope == null) {
                scope = symbolTable.rootScope();
            }
            Optional<Symbol> found = scope.lookup(name);
            if (found.isPresent()) {
                SymbolId resolvedId = found.get().id();
                results.add(ResolvedSymbolReference.resolved(
                        new SymbolReference(resolvedId, node.range(), SymbolReferenceKind.SIMPLE),
                        resolvedId
                ));
            } else {
                results.add(ResolvedSymbolReference.unresolved(
                        new SymbolReference(placeholderId(node), node.range(), SymbolReferenceKind.SIMPLE)
                ));
            }
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

        private static SymbolId placeholderId(AstNode node) {
            // For unresolved references a 0-owner placeholder carries the
            // reference's own range; the resolved entry never uses a placeholder.
            return SymbolId.of(0, node.range(), SymbolKind.TYPE);
        }

        private static boolean rangeContains(TextRange outer, TextRange inner) {
            return outer.startOffset() <= inner.startOffset() && inner.endOffset() <= outer.endOffset();
        }

        private static int area(TextRange r) {
            int size = r.endOffset() - r.startOffset();
            return size < 0 ? 0 : size;
        }
    }
}
