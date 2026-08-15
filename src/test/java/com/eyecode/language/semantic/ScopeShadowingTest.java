package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
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
 * Sprint 5.4b.3 — Scope Shadowing Hardening tests.
 * <p>
 * Validates the hierarchical lookup performed by
 * {@link JavaNameResolver#resolve(SymbolReference, SymbolTable)} under a
 * range of shadowing scenarios. Two structural limits of the current
 * {@code SymbolTableBuilder} shape which scenarios can actually be
 * exercised (and which cannot), without weakening any production code:
 *
 * <ul>
 *   <li><b>Single BLOCK scope per method</b> — the builder flattens all
 *       parameters and locals of a method (including locals declared in
 *       nested source-level blocks) into one BLOCK child of the METHOD
 *       scope. Java source-level "inner block shadows outer block"
 *       scenarios (spec sections 2 and 5) therefore cannot be expressed
 *       on the current model — those tests are {@code @Disabled} with the
 *       reason documented and will be re-enabled when the builder is
 *       extended to model per-source-block scopes.</li>
 *   <li><b>One symbol per name per scope</b> — {@code SymbolScopeImpl}'s
 *       {@code declare} throws on duplicates, so scenarios requiring two
 *       same-named declarations in the same BLOCK (e.g. a parameter and a
 *       same-named local in the same method) cannot be exercised directly.
 *       They are tested via the {@code TYPE-vs-BLOCK} shadowing
 *       (parameter shadows field / local shadows field) since that uses
 *       two separate scopes.</li>
 * </ul>
 * <p>
 * Source-string-driven; scope ids are derived dynamically from the symbol
 * table (no hardcoded ids).
 */
class ScopeShadowingTest {

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

    // ------------------------------------------------------------------
    // Scope helpers (mirrors SimpleNameResolverTest helpers)
    // ------------------------------------------------------------------

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
        return Math.max(r.endOffset() - r.startOffset(), 0);
    }

    private int indexOf(Pipeline p, String token) {
        int idx = p.source.indexOf(token);
        assertFalse(idx < 0, "token '" + token + "' not found in source");
        return idx;
    }

    private int nthIndexOf(Pipeline p, String token, int n) {
        int from = 0;
        for (int i = 1; i < n; i++) {
            int idx = p.source.indexOf(token, from);
            assertFalse(idx < 0, "occurrence #" + i + " of '" + token + "' not found");
            from = idx + 1;
        }
        int idx = p.source.indexOf(token, from);
        assertFalse(idx < 0, "occurrence #" + n + " of '" + token + "' not found");
        return idx;
    }

    // ------------------------------------------------------------------
    // 3. SHADOWING — LOCAL > FIELD
    // ------------------------------------------------------------------

    @Test
    void localShadowsField_whenInsideMethod() {
        String source = """
                class Example {
                    int value;

                    void test() {
                        int value = 10;
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int last = p.source.lastIndexOf("value");
        ResolvedSymbolReference r = resolveAt(p, "value", last);
        assertTrue(r.isResolved(), "value should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, r.resolvedSymbolId().kind(),
                "local inside method should shadow FIELD");
    }

    @Test
    void fieldResolvesWhenNoLocalShadows() {
        String source = """
                class Example {
                    int value;

                    void test() {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int second = nthIndexOf(p, "value", 2);
        ResolvedSymbolReference r = resolveAt(p, "value", second);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.FIELD, r.resolvedSymbolId().kind(),
                "without local shadow, value falls back to FIELD");
    }

    // The reference FROM outside the method (e.g. in a field initializer
    // or class-level expression) starts at the TYPE scope and so resolves
    // to FIELD — regardless of any same-named local inside a method.
    @Test
    void fieldResolvesForReferenceOutsideMethod() {
        String source = """
                class Example {
                    int value;
                    int other = value;

                    void test() {
                        int value = 10;
                    }
                }
                """;
        Pipeline p = build(source);
        // The reference `value` inside the field initializer is the
        // single occurrence guarded by `other =`. Confirms TYPE-level
        // fallback picks FIELD, not the LOCAL_VARIABLE declared in the
        // method body.
        int idx = p.source.indexOf("= value;");
        assertTrue(idx >= 0, "= value; not found");
        int refIdx = idx + "= ".length();
        ResolvedSymbolReference r = resolveAt(p, "value", refIdx);
        assertTrue(r.isResolved(), "field-initializer value should resolve");
        assertEquals(SymbolKind.FIELD, r.resolvedSymbolId().kind(),
                "reference outside method resolves to FIELD, not method-local");
    }

    // ------------------------------------------------------------------
    // 4. SHADOWING — PARAMETER > FIELD
    // ------------------------------------------------------------------

    @Test
    void parameterShadowsField_whenInsideMethod() {
        String source = """
                class Example {
                    int value;

                    void test(int value) {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int last = p.source.lastIndexOf("value");
        ResolvedSymbolReference r = resolveAt(p, "value", last);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.PARAMETER, r.resolvedSymbolId().kind(),
                "parameter inside method shadows FIELD");
    }

    @Test
    void fieldResolvesWhenNoParameterShadows() {
        String source = """
                class Example {
                    int value;

                    void testNoParam() {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int second = nthIndexOf(p, "value", 2);
        ResolvedSymbolReference r = resolveAt(p, "value", second);
        assertTrue(r.isResolved());
        assertEquals(SymbolKind.FIELD, r.resolvedSymbolId().kind());
    }

    // ------------------------------------------------------------------
    // 5. MULTI-LEVEL BLOCK-A/B/C
    // ------------------------------------------------------------------

    @Disabled("SymbolTableBuilder currently models all of a method's parameters and "
            + "locals inside a single BLOCK child of the METHOD scope. Source-level "
            + "nested blocks (BLOCK-A/B/C) do not become distinct scopes, so multi-level "
            + "BLOCK-shadowing cannot be exercised on this model. To be re-enabled when "
            + "the builder is extended to produce per-source-block scopes.")
    @Test
    void multiLevelBlockShadowsParent() {
        // PLACEHOLDER; this case will be re-enabled once the builder models
        // per-source-block nested BLOCK scopes. See test class Javadoc.
    }

    // ------------------------------------------------------------------
    // 6. SAME NAME IN DIFFERENT SCOPES — NEAREST MATCH WINS
    // ------------------------------------------------------------------

    @Test
    void sameNameInDifferentScopes_nearestWins() {
        // TYPE scope declares FIELD `x`. METHOD's BLOCK scope declares
        // PARAMETER `y` (no `x`) — a reference to `x` from inside METHOD
        // falls back through METHOD -> TYPE and finds FIELD. A separate
        // reference to `y` resolves to PARAMETER. The lookup stops at the
        // first match per name.
        String source = """
                class Example {
                    int x;
                    int y;

                    void test(int y) {
                        int a = x;
                        int b = y;
                    }
                }
                """;
        Pipeline p = build(source);

        // `x` inside body walks BLOCK -> METHOD -> TYPE -> hit FIELD `x`.
        int xRef = nthIndexOf(p, "x", 2); // 1st = "int x", 2nd = "= x"
        ResolvedSymbolReference xres = resolveAt(p, "x", xRef);
        assertTrue(xres.isResolved());
        assertEquals(SymbolKind.FIELD, xres.resolvedSymbolId().kind(),
                "x has no BLOCK/METHOD local, falls back to FIELD");

        // `y` inside body finds BLOCK PARAMETER (innermost wins over FIELD y).
        int yRef = nthIndexOf(p, "y", 3); // 1st=int y, 2nd=int y param, 3rd=use
        ResolvedSymbolReference yres = resolveAt(p, "y", yRef);
        assertTrue(yres.isResolved());
        assertEquals(SymbolKind.PARAMETER, yres.resolvedSymbolId().kind(),
                "y is PARAMETER nearest match in BLOCK");

        // Never produces AMBIGUOUS — innermost wins is deterministic.
        assertNotEquals(ResolutionKind.AMBIGUOUS, xres.resolutionKind());
        assertNotEquals(ResolutionKind.AMBIGUOUS, yres.resolutionKind());
    }

    @Disabled("SymbolTableBuilder does not currently declare parameters or locals "
            + "under TYPE or METHOD scopes; they all live in the single BLOCK child "
            + "of METHOD. The spec scenario 'same name in TYPE/METHOD/BLOCK' would "
            + "require parameters at METHOD and locals at distinct BLOCK scopes, "
            + "which the current model does not produce. Re-enable when builder is "
            + "extended.")
    @Test
    void sameNameInTypeMethodBlock_scopesThreeUsers() {
        // PLACEHOLDER; see Javadoc.
    }

    // ------------------------------------------------------------------
    // 7. READ-ONLY RESOLUTION — SymbolTable not mutated
    // ------------------------------------------------------------------

    @Test
    void resolutionIsReadOnly() {
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
        String before = structuralSignature(p.table);

        // Perform several resolutions across different names.
        int paramRef = nthIndexOf(p, "parameter", 2);
        resolveAt(p, "parameter", paramRef);
        int localRef = nthIndexOf(p, "local", 2);
        resolveAt(p, "local", localRef);
        int fieldRef = nthIndexOf(p, "field", 2);
        resolveAt(p, "field", fieldRef);
        resolveAt(p, "nonexistent", p.source.length() - 1);

        String after = structuralSignature(p.table);
        assertEquals(before, after,
                "SymbolTable structure must be unchanged by resolution (read-only)");
    }

    // ------------------------------------------------------------------
    // 8. UNRESOLVED SCOPE
    // ------------------------------------------------------------------

    @Test
    void nonexistentScopeIdResolvesUnresolved() {
        String source = """
                class Example {
                    int field;
                    void test(int parameter) {
                        int local = parameter;
                    }
                }
                """;
        Pipeline p = build(source);
        long bogusId = 999_999_999L;
        SymbolReference ref = SymbolReference.simple("missing", bogusId, TextRange.of(0, 1));
        ResolvedSymbolReference r = new JavaNameResolver().resolve(ref, p.table);
        assertNotNull(r);
        assertTrue(r.isUnresolved(),
                "nonexistent scopeId must yield UNRESOLVED, not throw");
        assertNull(r.resolvedSymbolId());
    }

    // ------------------------------------------------------------------
    // 9. REPEATED RESOLUTION — deterministic, no state change
    // ------------------------------------------------------------------

    @Test
    void repeatedResolutionIsDeterministic() {
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
        int paramRef = nthIndexOf(p, "parameter", 2);
        TextRange range = TextRange.of(paramRef, paramRef + "parameter".length());
        long scopeId = findInnermostContainingScope(p.table, range);
        SymbolReference ref = SymbolReference.simple("parameter", scopeId, range);
        JavaNameResolver resolver = new JavaNameResolver();

        ResolvedSymbolReference first = resolver.resolve(ref, p.table);
        ResolvedSymbolReference second = resolver.resolve(ref, p.table);
        ResolvedSymbolReference third = resolver.resolve(ref, p.table);

        assertEquals(first.resolutionKind(), second.resolutionKind());
        assertEquals(first.resolutionKind(), third.resolutionKind());
        assertEquals(first.resolvedSymbolId(), second.resolvedSymbolId());
        assertEquals(first.resolvedSymbolId(), third.resolvedSymbolId());
        assertTrue(first.isResolved());
        assertEquals(SymbolKind.PARAMETER, first.resolvedSymbolId().kind());

        // SymbolTable structural state must not change after repeated calls.
        String before = structuralSignature(p.table);
        resolver.resolve(ref, p.table);
        resolver.resolve(ref, p.table);
        resolver.resolve(ref, p.table);
        String after = structuralSignature(p.table);
        assertEquals(before, after,
                "repeated resolution must not mutate the SymbolTable");
    }

    // ------------------------------------------------------------------
    // 10. GOLDEN END-TO-END
    // ------------------------------------------------------------------

    @Test
    void goldenEndToEnd() {
        String source = """
                class Example {

                    int value;

                    void test(int value) {
                        int local = value;

                        {
                            int value2 = local;
                            value = value2;
                        }
                    }
                }
                """;
        Pipeline p = build(source);

        // value (parameter use inside method body) -> PARAMETER
        int paramUseIdx = nthIndexOf(p, "value", 2); // 1st field, 2nd param decl, 3rd use
        int paramUse = nthIndexOf(p, "value", 3);
        ResolvedSymbolReference valueParam = resolveAt(p, "value", paramUse);
        assertTrue(valueParam.isResolved(), "value (body use) should resolve");
        assertEquals(SymbolKind.PARAMETER, valueParam.resolvedSymbolId().kind(),
                "value inside body -> PARAMETER");

        // local -> LOCAL_VARIABLE
        int localUse = nthIndexOf(p, "local", 2);
        ResolvedSymbolReference localRef = resolveAt(p, "local", localUse);
        assertTrue(localRef.isResolved(), "local should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, localRef.resolvedSymbolId().kind(),
                "local -> LOCAL_VARIABLE");

        // value2 -> LOCAL_VARIABLE
        int value2Use = nthIndexOf(p, "value2", 2);
        ResolvedSymbolReference v2 = resolveAt(p, "value2", value2Use);
        assertTrue(v2.isResolved(), "value2 should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, v2.resolvedSymbolId().kind(),
                "value2 -> LOCAL_VARIABLE");

        // value (inside inner block re-assignment) -> PARAMETER (inner block
        // shares the method's single BLOCK scope; no distinct inner scope
        // exists, so it shadows via the same BLOCK which holds the parameter).
        int valueAssign = p.source.lastIndexOf("value");
        ResolvedSymbolReference valueAssignRef = resolveAt(p, "value", valueAssign);
        assertTrue(valueAssignRef.isResolved());
        assertEquals(SymbolKind.PARAMETER, valueAssignRef.resolvedSymbolId().kind(),
                "inner-block assignment of `value` resolves to PARAMETER (single BLOCK)");

        // Reference outside method (field initializer style) -> FIELD
        String outerSource = """
                class Example {
                    int value;
                    int init = value;

                    void test(int value) {
                    }
                }
                """;
        Pipeline op = build(outerSource);
        int fieldRef = op.source.indexOf("= value;");
        int refIdx = fieldRef + "= ".length();
        ResolvedSymbolReference fieldUse = resolveAt(op, "value", refIdx);
        assertTrue(fieldUse.isResolved(), "field-use should resolve");
        assertEquals(SymbolKind.FIELD, fieldUse.resolvedSymbolId().kind(),
                "reference outside method resolves to FIELD");
    }

    // ------------------------------------------------------------------
    // Structural signature utilities
    // ------------------------------------------------------------------

    /**
     * Builds a deterministic structural snapshot of a {@link SymbolTable}
     * — scope tree topology (kind+id+children) plus declared symbols per
     * scope (kind+name+range). Any mutation introduced by the resolver
     * (which this sprint forbids) will produce a different signature.
     */
    private static String structuralSignature(SymbolTable table) {
        StringBuilder sb = new StringBuilder();
        appendScope(table, table.rootScope(), sb, 0);
        return sb.toString();
    }

    private static void appendScope(SymbolTable table, SymbolScope scope, StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth))
                .append(scope.kind()).append('#').append(scope.id())
                .append(" range=").append(scope.range().startOffset())
                .append("..").append(scope.range().endOffset())
                .append(" symbols=[");
        boolean first = true;
        for (Symbol s : scope.declaredSymbols()) {
            if (!first) sb.append("|");
            sb.append(s.kind()).append(':').append(s.name())
                    .append('@').append(s.declarationRange().startOffset())
                    .append("..").append(s.declarationRange().endOffset());
            first = false;
        }
        sb.append("]\n");
        for (SymbolScope child : scope.children()) {
            appendScope(table, child, sb, depth + 1);
        }
    }
}
