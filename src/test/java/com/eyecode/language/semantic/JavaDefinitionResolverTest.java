package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.ProjectSymbolTable;
import com.eyecode.language.symbol.ScopeKind;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
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
 * Sprint 5.4c.1 — JavaDefinitionResolver tests.
 * <p>
 * Validates the Core "Go to Definition" foundation end-to-end across
 * the 9 supported {@link SymbolKind}s (LOCAL_VARIABLE / PARAMETER /
 * FIELD / TYPE / INTERFACE / ENUM / ANNOTATION / METHOD / CONSTRUCTOR)
 * plus the contract edge cases (unresolved, null, no mutation,
 * determinism, shadowing, qualified path).
 * <p>
 * Source-string-driven. Where the current {@link SymbolTableBuilder}
 * does not produce the relevant symbol kind directly
 * ({@code ANNOTATION} — no {@code ANNOTATION} case in
 * {@code TypeKind}; {@code CONSTRUCTOR} — only reachable via the
 * qualified member-lookup path), hand-built fixtures with
 * {@link ProjectSymbolTable} exercise the resolver directly.
 */
class JavaDefinitionResolverTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(sem.symbolTable(), source);
    }

    /**
     * DFS over the scope tree picking the best scope that contains the
     * given range. Mirrors {@code QualifiedNameResolverTest.scopeOf}.
     */
    private static SymbolScope innermostScopeContaining(SymbolTable table, TextRange refRange) {
        SymbolScope root = table.rootScope();
        SymbolScope best = null;
        int bestDepth = -1;
        int bestArea = Integer.MAX_VALUE;
        java.util.Deque<java.util.Map.Entry<SymbolScope, Integer>> stack = new java.util.ArrayDeque<>();
        stack.push(new java.util.AbstractMap.SimpleEntry<>(root, 0));
        while (!stack.isEmpty()) {
            java.util.Map.Entry<SymbolScope, Integer> e = stack.pop();
            SymbolScope scope = e.getKey();
            int depth = e.getValue();
            boolean holds = scope == root
                    || (scope.range().startOffset() <= refRange.startOffset()
                        && refRange.endOffset() <= scope.range().endOffset());
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

    private static int kindRank(SymbolScope scope) {
        return switch (scope.kind()) {
            case BLOCK -> 0;
            case METHOD -> 1;
            case TYPE -> 2;
            case PACKAGE -> 3;
            case ROOT -> 4;
        };
    }

    private int indexOf(Pipeline p, String token) {
        int idx = p.source.indexOf(token);
        assertFalse(idx < 0, "token '" + token + "' not found in source");
        return idx;
    }

    /**
     * Resolves a simple-name reference at the given textual position and
     * returns the resulting {@link DefinitionLocation} (or empty).
     */
    private Optional<DefinitionLocation> resolveSimpleAt(Pipeline p, String name, int refIdx) {
        TextRange refRange = TextRange.of(refIdx, refIdx + name.length());
        SymbolScope scope = innermostScopeContaining(p.table, refRange);
        SymbolReference ref = SymbolReference.simple(name, scope.id(), refRange);
        return new JavaDefinitionResolver().resolve(ref, p.table);
    }

    /**
     * Resolves a qualified-name reference at the given textual position
     * and returns the resulting {@link DefinitionLocation} (or empty).
     */
    private Optional<DefinitionLocation> resolveQualifiedAt(Pipeline p, String qnText, int refIdx) {
        TextRange refRange = TextRange.of(refIdx, refIdx + qnText.length());
        SymbolScope scope = innermostScopeContaining(p.table, refRange);
        SymbolReference ref = SymbolReference.qualified(qnText, scope.id(), refRange);
        return new JavaDefinitionResolver().resolve(ref, p.table);
    }

    // ============================================================================
    // Spec §5 list — 9 supported SymbolKinds
    // ============================================================================

    // 1. LOCAL_VARIABLE
    @Test
    void localVariable_resolvesToOwnDeclaration() {
        String source = """
                class Example {
                    void test() {
                        int local = 10;
                        int x = local;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("local");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "local", useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("local", l.symbol().name());
        assertEquals(SymbolKind.LOCAL_VARIABLE, l.symbol().kind());
        // The declarationRange points back to the local's declaration.
        // The symbol's own declarationRange is the source of truth.
        assertEquals(l.symbol().declarationRange(), l.declarationRange());
        // The declaration is somewhere in `int local = 10;` — assert
        // that the offset of the local-variable name (`local`) is
        // inside the declared range.
        int localNameIdx = p.source.indexOf("int local") + "int ".length();
        assertTrue(l.declarationRange().startOffset() <= localNameIdx);
        assertTrue(localNameIdx < l.declarationRange().endOffset());
    }

    // 2. PARAMETER
    @Test
    void parameter_resolvesToDeclaration() {
        String source = """
                class Example {
                    void test(int param) {
                        int x = param;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("param");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "param", useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("param", l.symbol().name());
        assertEquals(SymbolKind.PARAMETER, l.symbol().kind());
        // The symbol's own declarationRange is the source of truth.
        assertEquals(l.symbol().declarationRange(), l.declarationRange());
        // The PARAMETER name `param` is somewhere inside the declaration range.
        int paramNameIdx = p.source.indexOf("int param") + "int ".length();
        assertTrue(l.declarationRange().startOffset() <= paramNameIdx);
        assertTrue(paramNameIdx < l.declarationRange().endOffset());
    }

    // 3. FIELD
    @Test
    void field_resolvesToDeclaration() {
        String source = """
                class Example {
                    int field;
                    void test() {
                        int x = field;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("field");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "field", useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("field", l.symbol().name());
        assertEquals(SymbolKind.FIELD, l.symbol().kind());
        // The symbol's own declarationRange is the source of truth.
        assertEquals(l.symbol().declarationRange(), l.declarationRange());
        // The FIELD name `field` is somewhere inside the declaration range.
        int fieldNameIdx = p.source.indexOf("int field") + "int ".length();
        assertTrue(l.declarationRange().startOffset() <= fieldNameIdx);
        assertTrue(fieldNameIdx < l.declarationRange().endOffset());
    }

    // 4. TYPE (class)
    @Test
    void type_resolvesToDeclaration() {
        String source = """
                class MyClass {
                    int value;
                }
                class Use {
                    MyClass obj;
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass obj") + "MyClass".length() - "MyClass".length();
        // Anchor on the textual occurrence in "MyClass obj"
        refIdx = p.source.indexOf("MyClass obj");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "MyClass", refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("MyClass", l.symbol().name());
        assertEquals(SymbolKind.TYPE, l.symbol().kind());
        int declIdx = p.source.indexOf("class MyClass");
        assertEquals(declIdx, l.declarationRange().startOffset());
    }

    // 5. INTERFACE
    @Test
    void interface_resolvesToDeclaration() {
        String source = """
                interface RunnableLike {
                    int x = 0;
                }
                class Use {
                    RunnableLike ref;
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("RunnableLike ref");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "RunnableLike", refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("RunnableLike", l.symbol().name());
        assertEquals(SymbolKind.INTERFACE, l.symbol().kind());
        int declIdx = p.source.indexOf("interface RunnableLike");
        assertEquals(declIdx, l.declarationRange().startOffset());
    }

    // 6. ENUM
    @Test
    void enum_resolvesToDeclaration() {
        String source = """
                enum Color {
                    RED, GREEN, BLUE;
                    int v = 0;
                }
                class Use {
                    Color c;
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("Color c");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "Color", refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("Color", l.symbol().name());
        assertEquals(SymbolKind.ENUM, l.symbol().kind());
        int declIdx = p.source.indexOf("enum Color");
        assertEquals(declIdx, l.declarationRange().startOffset());
    }

    // 7. ANNOTATION — SymbolTableBuilder does not emit ANNOTATION symbols
    // (TypeKind has no ANNOTATION case). Use a hand-built fixture.
    @Test
    void annotation_resolvesToDeclaration() {
        SymbolTable table = new ProjectSymbolTable();
        SymbolScope root = table.rootScope();
        // Declare the ANNOTATION directly in the ROOT scope so the
        // simple-name lookup from the root succeeds.
        TextRange annRange = TextRange.of(10, 16);
        Symbol annotation = new Symbol(
                SymbolId.of(root.id(), annRange, SymbolKind.ANNOTATION),
                SymbolKind.ANNOTATION, "Marker", annRange,
                root.id(), root.id(), "Marker");
        ((ProjectSymbolTable) table).declareSymbol(root, annotation);

        TextRange refRange = TextRange.of(0, 6);
        SymbolReference ref = SymbolReference.simple("Marker", root.id(), refRange);
        Optional<DefinitionLocation> loc = new JavaDefinitionResolver().resolve(ref, table);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("Marker", l.symbol().name());
        assertEquals(SymbolKind.ANNOTATION, l.symbol().kind());
        assertEquals(annRange, l.declarationRange());
    }

    // 8. METHOD — unqualified method reference inside the same class.
    // Note: SymbolTableBuilder does NOT declare methods on the type by
    // simple name lookup from the type scope itself; methods are looked
    // up via the same hierarchical walk. An unqualified reference to a
    // method name from inside the same class resolves via the TYPE
    // scope (where the METHOD symbol is declared).
    @Test
    void method_resolvesToDeclaration() {
        String source = """
                class Example {
                    int helper() { return 1; }
                    void test() {
                        int x = helper();
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("helper()") + "helper".length() - "helper".length();
        // The first textual "helper" is in the declaration; the second
        // is in the call. Use the second one.
        int callIdx = p.source.lastIndexOf("helper");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "helper", callIdx);
        assertTrue(loc.isPresent(), "unqualified method call should resolve");
        DefinitionLocation l = loc.get();
        assertEquals("helper", l.symbol().name());
        assertEquals(SymbolKind.METHOD, l.symbol().kind());
        int declIdx = p.source.indexOf("int helper");
        assertEquals(declIdx, l.declarationRange().startOffset());
    }

    // 9. CONSTRUCTOR — SymbolTableBuilder declares CONSTRUCTOR symbols in
    // the TYPE scope. The simple-name path always finds the TYPE first
    // (declared in the package scope). Constructors are reachable via
    // the QUALIFIED path: TYPE.<ctor-name>. We hand-build a fixture that
    // exposes a CONSTRUCTOR via a TYPE scope so the resolver can find it.
    @Test
    void constructor_resolvesToDeclaration() {
        SymbolTable table = new ProjectSymbolTable();
        SymbolScope root = table.rootScope();
        ProjectSymbolTable mut = (ProjectSymbolTable) table;
        // typeScope range is disjoint from the refRange so the DFS picks
        // root as the starting scope — root.lookup("MyClass") finds the
        // TYPE first (declared in root), and member lookup on the TYPE
        // scope then locates the CONSTRUCTOR via findLocal.
        SymbolScope typeScope = mut.createChildScope(root, ScopeKind.TYPE,
                TextRange.of(200, 300));
        TextRange typeRange = TextRange.of(0, 50);
        Symbol type = new Symbol(
                SymbolId.of(root.id(), typeRange, SymbolKind.TYPE),
                SymbolKind.TYPE, "MyClass", typeRange,
                root.id(), typeScope.id(), "MyClass");
        mut.declareSymbol(root, type);
        TextRange ctorRange = TextRange.of(220, 240);
        Symbol ctor = new Symbol(
                SymbolId.of(typeScope.id(), ctorRange, SymbolKind.CONSTRUCTOR),
                SymbolKind.CONSTRUCTOR, "MyClass", ctorRange,
                typeScope.id(), typeScope.id(), "MyClass.MyClass");
        mut.declareSymbol(typeScope, ctor);

        TextRange refRange = TextRange.of(0, 7);
        SymbolReference ref = SymbolReference.constructorCall(
                "MyClass", root.id(), refRange);
        Optional<DefinitionLocation> loc = new JavaDefinitionResolver().resolve(ref, table);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("MyClass", l.symbol().name());
        assertEquals(SymbolKind.CONSTRUCTOR, l.symbol().kind());
        assertEquals(ctorRange, l.declarationRange());
    }

    // ============================================================================
    // Contract edge cases
    // ============================================================================

    // 10. Unknown symbol → empty
    @Test
    void nonexistentSymbol_resolvesToEmpty() {
        String source = """
                class Example {
                    void test() {
                        int x = nonexistent;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("nonexistent");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "nonexistent", refIdx);
        assertTrue(loc.isEmpty(),
                "unresolved reference yields Optional.empty() — no fabricated location");
    }

    // 11. Reference that does not exist in the source (e.g. built
    // manually with a bogus scope) → empty
    @Test
    void unresolvedReference_returnsEmpty() {
        String source = """
                class Example {
                    int field;
                    void test() {
                        int x = field;
                    }
                }
                """;
        Pipeline p = build(source);
        // Build a manual reference with a nonexistent scope id.
        SymbolReference ref = SymbolReference.simple(
                "field", 999_999_999L, TextRange.of(0, 5));
        Optional<DefinitionLocation> loc = new JavaDefinitionResolver().resolve(ref, p.table);
        assertTrue(loc.isEmpty());
    }

    // 12. Qualified reference — terminal resolves to its declaration
    @Test
    void qualifiedReference_resolvesToTerminalDeclaration() {
        String source = """
                class MyClass {
                    static int value;
                }
                class Use {
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.value");
        Optional<DefinitionLocation> loc = resolveQualifiedAt(p, "MyClass.value", refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("value", l.symbol().name());
        assertEquals(SymbolKind.FIELD, l.symbol().kind());
        int declIdx = p.source.indexOf("static int value");
        assertEquals(declIdx, l.declarationRange().startOffset());
    }

    // 13. Qualified reference with qualifier ok but terminal missing → empty
    @Test
    void qualifiedReference_missingTerminal_returnsEmpty() {
        String source = """
                class MyClass {
                    int value;
                }
                class Use {
                    void test() {
                        MyClass.unknown = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("MyClass.unknown");
        Optional<DefinitionLocation> loc = resolveQualifiedAt(p, "MyClass.unknown", refIdx);
        assertTrue(loc.isEmpty(),
                "missing terminal must not fabricate a location");
    }

    // 14. Shadowing — innermost declaration wins
    @Test
    void shadowing_returnsInnermostDeclaration() {
        String source = """
                class Example {
                    int value;
                    void test(int value) {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("value");
        Optional<DefinitionLocation> loc = resolveSimpleAt(p, "value", useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals(SymbolKind.PARAMETER, l.symbol().kind(),
                "innermost PARAMETER shadows outer FIELD");
        int paramDeclIdx = p.source.indexOf("int value)", p.source.indexOf("void test"))
                + "(".length();
        // The PARAMETER is declared at the int inside "(int value)"
        int paramStart = p.source.indexOf("int value", p.source.indexOf("void test"));
        assertEquals(paramStart, l.declarationRange().startOffset());
    }

    // 15. SymbolTable is not mutated
    @Test
    void symbolTableNotMutated() {
        String source = """
                class Example {
                    int field;
                    void test(int param) {
                        int local = 10;
                        int a = local;
                        int b = field;
                        int c = param;
                        int d = nonexistent;
                    }
                }
                """;
        Pipeline p = build(source);
        String before = structuralSignature(p.table);

        JavaDefinitionResolver resolver = new JavaDefinitionResolver();
        for (String name : List.of("local", "field", "param", "nonexistent")) {
            int idx = p.source.lastIndexOf(name);
            assertFalse(idx < 0, "missing " + name);
            TextRange range = TextRange.of(idx, idx + name.length());
            SymbolScope scope = innermostScopeContaining(p.table, range);
            SymbolReference ref = SymbolReference.simple(name, scope.id(), range);
            resolver.resolve(ref, p.table);
        }

        String after = structuralSignature(p.table);
        assertEquals(before, after,
                "SymbolTable structure must be unchanged by definition resolution");
    }

    // 16. Repeated resolution is deterministic
    @Test
    void repeatedResolutionIsDeterministic() {
        String source = """
                class Example {
                    static int field;
                }
                class Use {
                    void test() {
                        Example.field = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("Example.field");
        TextRange refRange = TextRange.of(refIdx, refIdx + "Example.field".length());
        SymbolScope scope = innermostScopeContaining(p.table, refRange);
        SymbolReference ref = SymbolReference.qualified(
                "Example.field", scope.id(), refRange);

        JavaDefinitionResolver resolver = new JavaDefinitionResolver();
        Optional<DefinitionLocation> a = resolver.resolve(ref, p.table);
        Optional<DefinitionLocation> b = resolver.resolve(ref, p.table);
        Optional<DefinitionLocation> c = resolver.resolve(ref, p.table);

        assertTrue(a.isPresent());
        assertEquals(a.get(), b.get());
        assertEquals(a.get(), c.get());
        assertEquals(a.get().symbol().name(), "field");
    }

    // 17. Null reference → NPE
    @Test
    void rejectsNullReference() {
        String source = "class Example { int field; }";
        Pipeline p = build(source);
        JavaDefinitionResolver resolver = new JavaDefinitionResolver();
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(null, p.table));
    }

    // 18. Null table → NPE
    @Test
    void rejectsNullTable() {
        SymbolReference ref = SymbolReference.simple(
                "x", 0L, TextRange.of(0, 1));
        JavaDefinitionResolver resolver = new JavaDefinitionResolver();
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(ref, null));
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) return count;
            count++;
            from = idx + 1;
        }
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
