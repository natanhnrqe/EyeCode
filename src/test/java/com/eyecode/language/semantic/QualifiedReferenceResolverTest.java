package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5.4b.7 — Qualified Reference Integration tests.
 * <p>
 * Validates that {@link QualifiedReferenceResolver} integrates the
 * qualified-name pipeline behind a real {@link SymbolReference}: a
 * caller hands it a reference of kind {@code QUALIFIED_NAME} and
 * obtains a {@link QualifiedReferenceResolution} carrying the
 * decomposed {@link QualifiedName} plus the qualifier/resolved
 * symbols — without mutating the AST, the {@link SymbolTable} or any
 * {@link Symbol}.
 * <p>
 * Source-string-driven. Scope ids and ranges are derived dynamically
 * from the symbol table (no hardcoded ids).
 */
class QualifiedReferenceResolverTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(sem.symbolTable(), source);
    }

    private SymbolScope scopeOf(Pipeline p, TextRange refRange) {
        // Mirrors QualifiedNameResolverTest.scopeOf — DFS preferring depth,
        // smaller area, inner kind (BLOCK > METHOD > TYPE > PACKAGE > ROOT).
        SymbolScope root = p.table.rootScope();
        SymbolScope best = null;
        int bestDepth = -1;
        int bestArea = Integer.MAX_VALUE;
        java.util.Deque<java.util.Map.Entry<SymbolScope, Integer>> stack = new java.util.ArrayDeque<>();
        stack.push(new java.util.AbstractMap.SimpleEntry<>(root, 0));
        while (!stack.isEmpty()) {
            java.util.Map.Entry<SymbolScope, Integer> e = stack.pop();
            SymbolScope scope = e.getKey();
            int depth = e.getValue();
            boolean holds = scope == root || contains(scope.range(), refRange);
            if (!holds) {
                continue;
            }
            int area = scope.range().endOffset() - scope.range().startOffset();
            boolean better;
            if (scope == root) {
                better = (best == null);
            } else if (depth > bestDepth) {
                better = true;
            } else if (depth == bestDepth) {
                if (area < bestArea) {
                    better = true;
                } else if (area == bestArea) {
                    better = kindRank(scope) < kindRank(best);
                } else {
                    better = false;
                }
            } else {
                better = false;
            }
            if (better) {
                best = scope;
                bestDepth = depth;
                bestArea = area;
            }
            for (SymbolScope c : scope.children()) {
                stack.push(new java.util.AbstractMap.SimpleEntry<>(c, depth + 1));
            }
        }
        return best != null ? best : root;
    }

    private static boolean contains(TextRange outer, TextRange inner) {
        return outer.startOffset() <= inner.startOffset()
                && inner.endOffset() <= outer.endOffset();
    }

    private static int kindRank(SymbolScope scope) {
        return switch (scope.kind()) {
            case BLOCK -> 0;
            case METHOD -> 1;
            case TYPE -> 2;
            case PACKAGE -> 3;
            case ROOT -> 4;
        };
    }

    /**
     * Builds a {@link SymbolReference} of kind {@code QUALIFIED_NAME}
     * for the textual reference {@code qnText} starting at
     * {@code refIdx} in {@code p.source}. Picks the innermost scope
     * that contains the reference range as the lookup start.
     */
    private SymbolReference qualifiedRef(Pipeline p, String qnText, int refIdx) {
        TextRange refRange = TextRange.of(refIdx, refIdx + qnText.length());
        SymbolScope scope = scopeOf(p, refRange);
        return SymbolReference.qualified(qnText, scope.id(), refRange);
    }

    private QualifiedReferenceResolution resolveChainAt(Pipeline p, String qnText, int refIdx) {
        SymbolReference ref = qualifiedRef(p, qnText, refIdx);
        SymbolScope scope = scopeOf(p, ref.range());
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        return new QualifiedReferenceResolver().resolve(ref, scope, lookup);
    }

    // ============================================================================
    // 1. SIMPLE_NAME does NOT enter the qualified resolver
    // ============================================================================
    @Test
    void simpleReference_doesNotEnterQualifiedResolver() {
        String source = """
                class MyClass {
                    int value;
                    void test() {
                        int local = value;
                    }
                }
                """;
        Pipeline p = build(source);
        // Build a SIMPLE_NAME reference (kind=SIMPLE_NAME) — the qualified
        // resolver must NOT try to resolve it; it returns UNRESOLVED with
        // empty qualified name (spec §3 — explicit separation).
        int refIdx = p.source.indexOf("value");
        TextRange range = TextRange.of(refIdx, refIdx + "value".length());
        SymbolScope scope = scopeOf(p, range);
        SymbolReference simpleRef = new SymbolReference(
                null, range, "value", scope.id(),
                com.eyecode.language.symbol.SymbolReferenceKind.SIMPLE_NAME);
        QualifiedReferenceResolution r = new QualifiedReferenceResolver()
                .resolve(simpleRef, scope, new ScopeBasedQualifiedMemberLookup(p.table));
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status(),
                "SIMPLE_NAME must not enter qualified resolver — caller routes to JavaNameResolver");
        assertTrue(r.qualifiedName().isEmpty());
        assertTrue(r.qualifierSymbol().isEmpty());
        assertTrue(r.resolvedSymbol().isEmpty());
        // The original reference is preserved for diagnostics.
        assertSame(simpleRef, r.reference());
    }

    // ============================================================================
    // 2. QUALIFIED reference — 2 components, both resolved
    // ============================================================================
    @Test
    void qualifiedReference_twoComponents_resolved() {
        String source = """
                class MyClass {
                    static int value;
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.value");
        QualifiedReferenceResolution r = resolveChainAt(p, "MyClass.value", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.RESOLVED, r.status());
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(2, qn.componentCount());
        assertEquals("MyClass", qn.qualifier().name());
        assertEquals("value", qn.terminalName().name());
        // Original reference is preserved
        assertNotNull(r.reference());
        assertEquals(com.eyecode.language.symbol.SymbolReferenceKind.QUALIFIED_NAME,
                r.reference().kind());
        // Qualifier + resolved
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("MyClass", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        Symbol resolved = r.resolvedSymbol().orElseThrow();
        assertEquals("value", resolved.name());
        assertEquals(SymbolKind.FIELD, resolved.kind());
    }

    // ============================================================================
    // 3. QUALIFIED reference — 3 components, all resolved
    // ============================================================================
    @Test
    void qualifiedReference_threeComponents_resolved() {
        String source = """
                class Outer {
                    static int bar;
                    class Inner {
                        static int baz;
                    }
                }
                """;
        Pipeline p = build(source);
        // The textual `Outer.Inner.baz` does not appear in the source — use
        // a manual QualifiedName at offset 0 (root scope as start).
        TextRange refRange = TextRange.of(0, "Outer.Inner.baz".length());
        SymbolScope root = p.table.rootScope();
        SymbolReference ref = SymbolReference.qualified(
                "Outer.Inner.baz", root.id(), refRange);
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        QualifiedReferenceResolution r = new QualifiedReferenceResolver()
                .resolve(ref, root, lookup);

        assertEquals(QualifiedReferenceResolution.ResolutionStatus.RESOLVED, r.status());
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(3, qn.componentCount());
        assertEquals("Outer", qn.qualifier().name());
        assertEquals("Inner", qn.component(1).name());
        assertEquals("baz", qn.terminalName().name());
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("Outer", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        Symbol resolved = r.resolvedSymbol().orElseThrow();
        assertEquals("baz", resolved.name());
        assertEquals(SymbolKind.FIELD, resolved.kind());
    }

    // ============================================================================
    // 4. qualifier resolved + terminal resolved (golden MyClass.value)
    // ============================================================================
    @Test
    void goldenMyClassValue() {
        String source = """
                class MyClass {
                    static int value;
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.value");
        QualifiedReferenceResolution r = resolveChainAt(p, "MyClass.value", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.RESOLVED, r.status());
        // qualifier: TYPE MyClass
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("MyClass", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        // resolved: FIELD value
        Symbol resolved = r.resolvedSymbol().orElseThrow();
        assertEquals("value", resolved.name());
        assertEquals(SymbolKind.FIELD, resolved.kind());
        // qualifiedName preserved
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals("MyClass.value", join(qn));
    }

    // ============================================================================
    // 5. qualifier resolved + terminal missing (UNRESOLVED, qualifier preserved)
    // ============================================================================
    @Test
    void qualifierResolved_terminalMissing_preservesQualifier() {
        String source = """
                class MyClass {
                    int value;
                    void test() {
                        MyClass.unknown = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.unknown");
        QualifiedReferenceResolution r = resolveChainAt(p, "MyClass.unknown", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status());
        // Qualifier resolved (spec §9 — diagnostic preservation)
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("MyClass", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        // resolvedSymbol also carries the qualifier for diagnostics
        assertTrue(r.resolvedSymbol().isPresent());
        assertEquals("MyClass", r.resolvedSymbol().orElseThrow().name());
        // qualifiedName preserved
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(2, qn.componentCount());
        assertEquals("MyClass", qn.qualifier().name());
        assertEquals("unknown", qn.terminalName().name());
    }

    // ============================================================================
    // 6. first component missing (UNRESOLVED, no symbol fabricated)
    // ============================================================================
    @Test
    void firstComponentMissing_noSymbolFabricated() {
        String source = """
                class MyClass {
                    int value;
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.value");
        // Use a non-existent qualifier "Unknown"
        QualifiedReferenceResolution r = resolveChainAt(p, "Unknown.bar", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status());
        assertTrue(r.qualifierSymbol().isEmpty(),
                "no symbol is fabricated when the first lookup fails");
        assertTrue(r.resolvedSymbol().isEmpty(),
                "no symbol is fabricated when the first lookup fails");
        // qualifiedName preserved (the original syntactic structure remains)
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals("Unknown", qn.qualifier().name());
        assertEquals("bar", qn.terminalName().name());
    }

    // ============================================================================
    // 7. second component missing in a 3-component chain
    // ============================================================================
    @Test
    void secondComponentMissing_threeComponentChain() {
        String source = """
                class Outer {
                    class Inner {
                        int baz;
                    }
                }
                """;
        Pipeline p = build(source);
        // Outer resolves to TYPE; "unknown" missing; "baz" never asked.
        TextRange refRange = TextRange.of(0, "Outer.unknown.baz".length());
        SymbolReference ref = SymbolReference.qualified(
                "Outer.unknown.baz", p.table.rootScope().id(), refRange);
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        QualifiedReferenceResolution r = new QualifiedReferenceResolver()
                .resolve(ref, p.table.rootScope(), lookup);

        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status());
        // The leftmost resolved symbol (Outer) is preserved for diagnostics
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("Outer", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        assertEquals("Outer", r.resolvedSymbol().orElseThrow().name());
        // 3 components preserved in qualifiedName
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(3, qn.componentCount());
        assertEquals("Outer", qn.qualifier().name());
        assertEquals("unknown", qn.component(1).name());
        assertEquals("baz", qn.terminalName().name());
    }

    // ============================================================================
    // 8. chain short-circuits at first failure — last successful symbol preserved
    // ============================================================================
    @Test
    void chainShortCircuitsAtFirstFailure_lastSymbolPreserved() {
        // Use a chain that succeeds for `Outer` then fails at `unknown`.
        // The resolver must NOT continue searching past the first failure;
        // the last successful symbol is preserved.
        String source = """
                class Outer {
                    class Inner {
                        int baz;
                    }
                }
                """;
        Pipeline p = build(source);
        TextRange refRange = TextRange.of(0, "Outer.unknown.baz".length());
        SymbolReference ref = SymbolReference.qualified(
                "Outer.unknown.baz", p.table.rootScope().id(), refRange);
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        QualifiedReferenceResolution r = new QualifiedReferenceResolver()
                .resolve(ref, p.table.rootScope(), lookup);
        // The chain stopped at `unknown` — neither `baz` nor anything beyond.
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status());
        // The terminal component "baz" must NOT appear in resolvedSymbol
        Symbol last = r.resolvedSymbol().orElseThrow();
        assertEquals("Outer", last.name(),
                "the last SUCCESSFUL step is preserved, not the unresolved terminal");
        // The qualifier symbol is the same as the last resolved symbol
        assertEquals(last, r.qualifierSymbol().orElseThrow());
    }

    // ============================================================================
    // 9. ranges are preserved end-to-end (no shifting, no truncation)
    // ============================================================================
    @Test
    void rangesPreservedEndToEnd() {
        String source = """
                class MyClass {
                    int value;
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.value");
        QualifiedReferenceResolution r = resolveChainAt(p, "MyClass.value", refIdx);
        // The original reference range is preserved as-is
        TextRange originalRange = r.reference().range();
        assertEquals(refIdx, originalRange.startOffset());
        assertEquals(refIdx + "MyClass.value".length(), originalRange.endOffset());
        // The decomposed QualifiedName ranges are derived from the original
        // base offset (refIdx) — they must align exactly with the source.
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(TextRange.of(refIdx, refIdx + "MyClass".length()),
                qn.qualifier().range());
        assertEquals(TextRange.of(refIdx + "MyClass.".length(),
                                  refIdx + "MyClass.value".length()),
                qn.terminalName().range());
        // The full QualifiedName range spans the entire text
        assertEquals(originalRange, qn.range());
    }

    // ============================================================================
    // 10. SymbolTable is NOT mutated across mixed resolutions
    // ============================================================================
    @Test
    void symbolTableNotMutated() {
        String source = """
                class MyClass {
                    int value;
                    void test() {
                        MyClass.value = 1;
                        MyClass.unknown = 1;
                        Unknown.bar = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        String before = structuralSignature(p.table);

        QualifiedReferenceResolver resolver = new QualifiedReferenceResolver();
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        for (String qnText : List.of("MyClass.value", "MyClass.unknown", "Unknown.bar")) {
            int idx = p.source.indexOf(qnText);
            assertFalse(idx < 0, "missing " + qnText);
            SymbolReference ref = qualifiedRef(p, qnText, idx);
            SymbolScope scope = scopeOf(p, ref.range());
            resolver.resolve(ref, scope, lookup);
        }

        String after = structuralSignature(p.table);
        assertEquals(before, after,
                "SymbolTable structure must be unchanged by qualified-reference resolution");
    }

    // ============================================================================
    // 11. repeated resolution is deterministic
    // ============================================================================
    @Test
    void repeatedResolutionIsDeterministic() {
        String source = """
                class MyClass {
                    static int value;
                }
                """;
        Pipeline p = build(source);
        TextRange refRange = TextRange.of(0, "MyClass.value".length());
        SymbolReference ref = SymbolReference.qualified(
                "MyClass.value", p.table.rootScope().id(), refRange);
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        QualifiedReferenceResolver resolver = new QualifiedReferenceResolver();

        QualifiedReferenceResolution a = resolver.resolve(ref, p.table.rootScope(), lookup);
        QualifiedReferenceResolution b = resolver.resolve(ref, p.table.rootScope(), lookup);
        QualifiedReferenceResolution c = resolver.resolve(ref, p.table.rootScope(), lookup);

        assertEquals(a.status(), b.status());
        assertEquals(a.status(), c.status());
        assertEquals(a.qualifierSymbol(), b.qualifierSymbol());
        assertEquals(a.resolvedSymbol(), b.resolvedSymbol());
        assertEquals(a.qualifiedName(), b.qualifiedName());
        assertTrue(a.isResolved());
        assertEquals("value", a.resolvedSymbol().orElseThrow().name());
    }

    // ============================================================================
    // 12. integration with a real SymbolReference built from the source
    // ============================================================================
    @Test
    void integrationWithRealSymbolReference() {
        // End-to-end: build a file with a nested type whose member is
        // referenced via a 3-component qualified name from a method.
        String source = """
                class Outer {
                    class Inner {
                        static int baz;
                    }
                }
                class Use {
                    void test() {
                        Outer.Inner.baz = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        // `Outer.Inner.baz` appears in the source once.
        int refIdx = p.source.indexOf("Outer.Inner.baz");
        QualifiedReferenceResolution r = resolveChainAt(p, "Outer.Inner.baz", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.RESOLVED, r.status());
        // Reference was real and is preserved
        assertEquals(com.eyecode.language.symbol.SymbolReferenceKind.QUALIFIED_NAME,
                r.reference().kind());
        assertEquals("Outer.Inner.baz", r.reference().name());
        // Qualifier resolved to Outer (TYPE)
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("Outer", qualifier.name());
        assertEquals(SymbolKind.TYPE, qualifier.kind());
        // Terminal resolved to baz (FIELD on Inner)
        Symbol resolved = r.resolvedSymbol().orElseThrow();
        assertEquals("baz", resolved.name());
        assertEquals(SymbolKind.FIELD, resolved.kind());
        // Qualified name has 3 components
        QualifiedName qn = r.qualifiedName().orElseThrow();
        assertEquals(3, qn.componentCount());
        assertEquals("Outer", qn.qualifier().name());
        assertEquals("Inner", qn.component(1).name());
        assertEquals("baz", qn.terminalName().name());
    }

    // ============================================================================
    // 13. local-variable qualifier — no type info, chain stops at member step
    // ============================================================================
    @Test
    void localVariableQualifier_chainStopsAtMemberStep() {
        // LOCAL_VARIABLE has no member context in the current model → the
        // member step fails. Qualifier IS resolved (it's a real local
        // variable); the chain yields UNRESOLVED with the qualifier
        // preserved in both optionals for diagnostics.
        String source = """
                class MyClass {
                    static int value;
                    void test() {
                        int obj = 0;
                        obj.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.lastIndexOf("obj.value");
        QualifiedReferenceResolution r = resolveChainAt(p, "obj.value", refIdx);
        assertEquals(QualifiedReferenceResolution.ResolutionStatus.UNRESOLVED, r.status());
        Symbol qualifier = r.qualifierSymbol().orElseThrow();
        assertEquals("obj", qualifier.name());
        assertEquals(SymbolKind.LOCAL_VARIABLE, qualifier.kind());
        // resolvedSymbol also carries the qualifier (diagnostic preservation
        // rule — spec §9). The two optionals intentionally hold the same
        // Symbol instance when the chain stops at the member step.
        Symbol last = r.resolvedSymbol().orElseThrow();
        assertEquals(qualifier, last);
        assertEquals("obj", last.name());
        // The unresolved terminal `value` must NOT appear as a resolved symbol
        assertFalse("value".equals(last.name()),
                "the unresolved terminal must NOT appear as a resolved symbol");
    }

    // ============================================================================
    // 14. null-argument rejection
    // ============================================================================
    @Test
    void rejectsNullArguments() {
        QualifiedReferenceResolver resolver = new QualifiedReferenceResolver();
        String source = "class MyClass { int value; }";
        Pipeline p = build(source);
        SymbolReference ref = SymbolReference.qualified(
                "MyClass.value", p.table.rootScope().id(),
                TextRange.of(0, "MyClass.value".length()));
        QualifiedMemberLookup lookup = new ScopeBasedQualifiedMemberLookup(p.table);
        SymbolScope scope = p.table.rootScope();
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(null, scope, lookup));
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(ref, null, lookup));
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(ref, scope, null));
    }

    // ============================================================================
    // 15. SymbolReference.qualified validates non-empty + at-least-one-dot
    // ============================================================================
    @Test
    void qualifiedFactoryValidatesInput() {
        TextRange range = TextRange.of(0, 3);
        // non-empty, no dot → IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> SymbolReference.qualified("foo", 0L, range));
        // empty → IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> SymbolReference.qualified("", 0L, range));
        // null name → NullPointerException
        assertThrows(NullPointerException.class,
                () -> SymbolReference.qualified(null, 0L, range));
        // null range → NullPointerException
        assertThrows(NullPointerException.class,
                () -> SymbolReference.qualified("foo.bar", 0L, null));
        // happy path: at least one dot
        SymbolReference ok = SymbolReference.qualified("foo.bar", 0L,
                TextRange.of(0, 7));
        assertEquals(com.eyecode.language.symbol.SymbolReferenceKind.QUALIFIED_NAME, ok.kind());
        assertEquals("foo.bar", ok.name());
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private static String join(QualifiedName qn) {
        StringBuilder sb = new StringBuilder();
        List<QualifiedNameComponent> comps = qn.components();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) sb.append('.');
            sb.append(comps.get(i).name());
        }
        return sb.toString();
    }

    private static String structuralSignature(SymbolTable table) {
        StringBuilder sb = new StringBuilder();
        appendScope(table.rootScope(), sb, 0);
        return sb.toString();
    }

    private static void appendScope(SymbolScope scope, StringBuilder sb, int depth) {
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
            appendScope(child, sb, depth + 1);
        }
    }
}
