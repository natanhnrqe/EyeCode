package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.ScopeKind;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for simple-name resolution via the single-reference entry
 * point {@link NameResolver#resolve(SymbolReference, SymbolTable)}
 * (Sprint 5.4b.2).
 * <p>
 * Every test pipelined:
 * <pre>
 *   source -> JavaParser -> SymbolTableBuilder -> SymbolTable
 *         -> JavaNameResolver.resolve(reference, table)
 * </pre>
 * SymbolReference inputs carry the name and the scope id of the innermost
 * scope that contains the reference occurrence (derived dynamically from
 * the symbol table; no hardcoded ids).
 * <p>
 * Resolves only what the current model can represent: LOCAL_VARIABLE,
 * PARAMETER, FIELD, TYPE, INTERFACE, ENUM, METHOD, CONSTRUCTOR.
 * TYPE_PARAMETER is declared as supported by the spec but the model does
 * not yet produce such symbols, so its case is disabled.
 */
class SimpleNameResolverTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(sem.symbolTable(), source);
    }

    private ResolvedSymbolReference resolveAt(Pipeline p, String name, int refOffset) {
        TextRange refRange = TextRange.of(refOffset, refOffset + name.length());
        long scopeId = findInnermostContainingScope(p.table, refRange);
        SymbolReference ref = SymbolReference.simple(name, scopeId, refRange);
        return new JavaNameResolver().resolve(ref, p.table);
    }

    /**
     * Locates the innermost scope whose range contains the given reference
     * range, walking the scope tree from the root. When two scopes contain
     * the range with equal area (e.g. METHOD and its BLOCK child share the
     * same source range in this model), the deeper one in the tree wins
     * so the reference starts its hierarchical lookup at the most nested
     * scope — this is what makes the BLOCK -> METHOD -> TYPE chain
     * effective for shadowing.
     */
    private static long findInnermostContainingScope(SymbolTable table, TextRange refRange) {
        SymbolScope root = table.rootScope();
        ScopeAndDepth best = new ScopeAndDepth(root, 0);
        for (SymbolScope child : root.children()) {
            ScopeAndDepth cand = innermost(child, refRange, 1);
            if (cand != null
                    && (cand.depth() > best.depth()
                        || (cand.depth() == best.depth()
                            && area(cand.scope().range()) < area(best.scope().range())))) {
                best = cand;
            }
        }
        return best.scope().id();
    }

    private record ScopeAndDepth(SymbolScope scope, int depth) {}

    private static ScopeAndDepth innermost(SymbolScope scope, TextRange refRange, int depth) {
        if (!contains(scope.range(), refRange)) {
            return null;
        }
        ScopeAndDepth best = new ScopeAndDepth(scope, depth);
        for (SymbolScope child : scope.children()) {
            ScopeAndDepth cand = innermost(child, refRange, depth + 1);
            if (cand == null) {
                continue;
            }
            if (cand.depth() > best.depth()
                    || (cand.depth() == best.depth()
                        && area(cand.scope().range()) < area(best.scope().range()))) {
                best = cand;
            }
        }
        return best;
    }

    private static boolean contains(TextRange outer, TextRange inner) {
        return outer.startOffset() <= inner.startOffset()
                && inner.endOffset() <= outer.endOffset();
    }

    private static int area(TextRange r) {
        int a = r.endOffset() - r.startOffset();
        return Math.max(a, 0);
    }

    private int indexOf(Pipeline p, String token) {
        int idx = p.source.indexOf(token);
        assertFalse(idx < 0, "token '" + token + "' not found in source");
        return idx;
    }

    private Symbol findSymbolByName(Pipeline p, String name) {
        SymbolScope root = p.table.rootScope();
        Optional<Symbol> direct = root.findLocal(name);
        if (direct.isPresent()) return direct.get();
        // Walk scope tree; symbols declared at any scope.
        for (SymbolScope scope : allScopes(root)) {
            for (Symbol s : scope.declaredSymbols()) {
                if (s.name().equals(name)) return s;
            }
        }
        throw new AssertionError("symbol '" + name + "' not declared in symbol table");
    }

    private List<SymbolScope> allScopes(SymbolScope root) {
        List<SymbolScope> out = new ArrayList<>();
        out.add(root);
        for (SymbolScope child : root.children()) {
            out.addAll(allScopes(child));
        }
        return out;
    }

    // ------------------------------------------------------------------

    // 1. local variable encontrada no proprio scope ----------------------------
    @Test
    void localVariableFoundInOwnScope() {
        String source = """
                class A {
                    void test() {
                        int local = 1;
                        int other = local;
                    }
                }
                """;
        Pipeline p = build(source);
        int refOffset = indexOf(p, "local;");
        // The use "local;" on the RHS points at the occurrence after `other = `.
        // Identify the second occurrence of "local".
        int first = p.source.indexOf("local");
        int second = p.source.indexOf("local", first + 1);
        ResolvedSymbolReference r = resolveAt(p, "local", second);
        assertTrue(r.isResolved(), "local use should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, r.resolvedSymbolId().kind(),
                "local -> LOCAL_VARIABLE");
    }

    // 2. parameter encontrado dentro do method scope --------------------------
    @Test
    void parameterFoundInsideMethodScope() {
        String source = """
                class A {
                    void test(int parameter) {
                        int x = parameter;
                    }
                }
                """;
        Pipeline p = build(source);
        int first = p.source.indexOf("parameter");
        int second = p.source.indexOf("parameter", first + 1);
        ResolvedSymbolReference r = resolveAt(p, "parameter", second);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.PARAMETER, r.resolvedSymbolId().kind());
    }

    // 3. field encontrado atraves do scope do type ----------------------------
    @Test
    void fieldFoundThroughTypeScope() {
        String source = """
                class A {
                    int value;
                    void test() {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int first = p.source.indexOf("value");
        int second = p.source.indexOf("value", first + 1);
        ResolvedSymbolReference r = resolveAt(p, "value", second);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.FIELD, r.resolvedSymbolId().kind());
    }

    // 4. type encontrado atraves do scope apropriado ---------------------------
    @Test
    void typeFoundThroughScope() {
        String source = """
                class A {
                    A clone() {
                        return this;
                    }
                }
                """;
        Pipeline p = build(source);
        // Reference the type name "A" in the return type position
        int idx = indexOf(p, "A clone");
        ResolvedSymbolReference r = resolveAt(p, "A", idx);
        assertTrue(r.isResolved(), "type name should resolve to a TYPE symbol");
        Symbol resolved = p.table.find(r.resolvedSymbolId()).orElseThrow();
        assertEquals("A", resolved.name());
    }

    // 5. type parameter encontrado -------------------------------------------
    @Disabled("SymbolTableBuilder does not yet produce TYPE_PARAMETER symbols")
    @Test
    void typeParameterFound() {
        // Placeholder: requires SymbolTableBuilder to register type-parameter
        // declarations on the type scope. Not implemented in 5.4a; deferred to
        // a later sub-sprint that extends the builder.
    }

    // 6. method encontrado ---------------------------------------------------
    @Test
    void methodFound() {
        String source = """
                class A {
                    void run() {
                    }
                    void test() {
                        run();
                    }
                }
                """;
        Pipeline p = build(source);
        int first = p.source.indexOf("run");
        int third = p.source.indexOf("run", first + 5);
        ResolvedSymbolReference r = resolveAt(p, "run", third);
        assertTrue(r.isResolved(), "unqualified method name should resolve");
        assertEquals(SymbolKind.METHOD, r.resolvedSymbolId().kind());
    }

    // 7. constructor encontrado ----------------------------------------------
    @Test
    void constructorFound() {
        String source = """
                class A {
                    A() {}
                }
                """;
        Pipeline p = build(source);
        // First occurrence of "A" is the class declaration; second is the
        // constructor name. Resolving "A" inside the constructor's METHOD
        // scope walks to the TYPE scope, where the CONSTRUCTOR symbol is
        // declared.
        int first = p.source.indexOf("A");
        int second = p.source.indexOf("A", first + 1);
        ResolvedSymbolReference r = resolveAt(p, "A", second);
        assertTrue(r.isResolved(), "constructor name 'A' should resolve");
        assertEquals(SymbolKind.CONSTRUCTOR, r.resolvedSymbolId().kind(),
                "name inside a constructor scope should resolve to CONSTRUCTOR, got "
                        + r.resolvedSymbolId().kind());
    }

    // 8. nome inexistente -> UNRESOLVED --------------------------------------
    @Test
    void unknownNameIsUnresolved() {
        String source = """
                class A {
                    void test() {
                        int x = zzz;
                    }
                }
                """;
        Pipeline p = build(source);
        int idx = indexOf(p, "zzz");
        ResolvedSymbolReference r = resolveAt(p, "zzz", idx);
        assertTrue(r.isUnresolved(), "zzz should be UNRESOLVED");
        assertNull(r.resolvedSymbolId());
    }

    // 9. shadowing: local variable shadows field -----------------------------
    @Test
    void shadowingLocalShadowsField() {
        String source = """
                class Example {
                    int value;

                    void test() {
                        int value;
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        // The use of `value` on the RHS of `int x = value;` should resolve
        // to the LOCAL_VARIABLE, not the FIELD.
        int last = p.source.lastIndexOf("value");
        ResolvedSymbolReference r = resolveAt(p, "value", last);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.LOCAL_VARIABLE, r.resolvedSymbolId().kind(),
                "inner `value` use should resolve to LOCAL_VARIABLE (shadowing), got "
                        + r.resolvedSymbolId().kind());
    }

    // 10. fallback para parent scope (field encontrado do method) --------------
    @Test
    void fallbackToParentScope() {
        String source = """
                class Example {
                    int value;

                    void test() {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int first = p.source.indexOf("value");
        int second = p.source.indexOf("value", first + 1);
        ResolvedSymbolReference r = resolveAt(p, "value", second);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.FIELD, r.resolvedSymbolId().kind(),
                "when no local shadows, `value` should fall back to FIELD");
    }

    // 11. shadowing em multiplos niveis (TYPE/METHOD/BLOCK) -------------------
    @Test
    void multiLevelShadowingFollowsHierarchy() {
        // If a name is in METHOD but not in BLOCK, lookup should find METHOD;
        // if not in METHOD, look in TYPE.
        // Parameters live in the BLOCK scope child of METHOD (per
        // SymbolTableBuilder), so a reference uses BLOCK scope and lookup
        // walks BLOCK -> METHOD -> TYPE.
        String source = """
                class A {
                    int value;

                    void test(int parameter) {
                        int x = parameter;
                        int y = value;
                    }
                }
                """;
        Pipeline p = build(source);
        // parameter: should walk BLOCK -> METHOD (declared in BLOCK) -> resolve to PARAMETER.
        int pIdx = p.source.lastIndexOf("parameter");
        ResolvedSymbolReference pRef = resolveAt(p, "parameter", pIdx);
        assertTrue(pRef.isResolved());
        assertEquals(SymbolKind.PARAMETER, pRef.resolvedSymbolId().kind(),
                "parameter should resolve through BLOCK -> chain to PARAMETER");

        // value: should walk BLOCK -> METHOD (no value) -> TYPE (value) -> resolve to FIELD.
        int vIdx = p.source.lastIndexOf("value");
        ResolvedSymbolReference vRef = resolveAt(p, "value", vIdx);
        assertTrue(vRef.isResolved());
        assertEquals(SymbolKind.FIELD, vRef.resolvedSymbolId().kind(),
                "value should fall back through BLOCK -> METHOD -> TYPE to FIELD");
    }

    // 12. referencia permanece imutavel apos resolucao -----------------------
    @Test
    void referenceRemainsImmutableAfterResolution() {
        String source = """
                class A {
                    int value;
                    void test() {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int first = p.source.indexOf("value");
        int second = p.source.indexOf("value", first + 1);
        TextRange range = TextRange.of(second, second + "value".length());
        long scopeId = findInnermostContainingScope(p.table, range);
        SymbolReference ref = SymbolReference.simple("value", scopeId, range);

        ResolvedSymbolReference r = new JavaNameResolver().resolve(ref, p.table);
        assertTrue(r.isResolved());

        // The original reference must be unchanged after resolution
        assertEquals("value", ref.name(), "name unchanged");
        assertEquals(scopeId, ref.scopeId(), "scopeId unchanged");
        assertEquals(range, ref.range(), "range unchanged");
    }

    // ------------------------------------------------------------------
    // TESTE GOLDEN end-to-end
    // ------------------------------------------------------------------

    @Test
    void goldenEndToEnd() {
        String source = """
                class Example {
                    int field;

                    void test(int parameter) {
                        int local = parameter;
                        local = field;
                    }
                }
                """;
        Pipeline p = build(source);

        // parameter -> PARAMETER
        int pIdx = p.source.indexOf("parameter", p.source.indexOf("int parameter") + 1);
        ResolvedSymbolReference param = resolveAt(p, "parameter", pIdx);
        assertTrue(param.isResolved(), "parameter should resolve");
        assertEquals(SymbolKind.PARAMETER, param.resolvedSymbolId().kind(),
                "parameter -> PARAMETER");

        // local -> LOCAL_VARIABLE
        int localUseIdx = p.source.indexOf("local", p.source.indexOf("int local") + 1);
        ResolvedSymbolReference local = resolveAt(p, "local", localUseIdx);
        assertTrue(local.isResolved(), "local should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, local.resolvedSymbolId().kind(),
                "local -> LOCAL_VARIABLE");

        // field -> FIELD
        int fieldUseIdx = p.source.indexOf("field", p.source.indexOf("int field") + 1);
        ResolvedSymbolReference field = resolveAt(p, "field", fieldUseIdx);
        assertTrue(field.isResolved(), "field should resolve");
        assertEquals(SymbolKind.FIELD, field.resolvedSymbolId().kind(),
                "field -> FIELD");
    }

    // ------------------------------------------------------------------
    // Bonus: null rejection
    // ------------------------------------------------------------------

    @Test
    void nullArgumentsRejected() {
        String source = "class A {}";
        Pipeline p = build(source);
        SymbolReference ref = SymbolReference.simple(
                "x", p.table.rootScope().id(), TextRange.of(0, 1));
        assertThrows(() -> new JavaNameResolver().resolve((SymbolReference) null, p.table),
                NullPointerException.class);
        assertThrows(() -> new JavaNameResolver().resolve(ref, null),
                NullPointerException.class);
    }

    private static void assertThrows(java.lang.Runnable fn, Class<? extends Exception> type) {
        try {
            fn.run();
        } catch (Exception e) {
            assertTrue(type.isInstance(e),
                    "expected " + type.getSimpleName() + " but got " + e.getClass().getName());
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }
}
