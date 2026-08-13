package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.ProjectSymbolTable;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused contract tests for the {@link NameResolver} interface and the
 * {@link JavaNameResolver} implementation (Sprint 5.4b.1).
 * <p>
 * Each test pipelined builds a {@link ParserSnapshot} from a small Java source,
 * builds a {@link SymbolTable}, runs {@link JavaNameResolver#resolve}, and
 * asserts on the QUALITATIVE resolution outcome. Ranges and offsets are never
 * hard-coded — assertions read the source text and the AST to find references.
 */
class NameResolverTest {

    private record Pipeline(ParserSnapshot parserSnapshot, SymbolTable symbolTable) {}

    private Pipeline buildPipeline(String source) {
        JavaLexerService lexerService = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexerService.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        AstNode astRoot = model.getAstRoot();
        ParserSnapshot parserSnapshot = new ParserSnapshot(1, source, astRoot);
        SemanticModelSnapshot semantic = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(parserSnapshot, semantic.symbolTable());
    }

    private List<ResolvedSymbolReference> resolve(String source) {
        Pipeline p = buildPipeline(source);
        return new JavaNameResolver().resolve(p.parserSnapshot, p.symbolTable);
    }

    /** Returns only resolved results whose referenced name matches one of the supplied names. */
    private List<ResolvedSymbolReference> resolvedWithNames(List<ResolvedSymbolReference> results, String source, String... names) {
        String haystackLower = source;
        java.util.Set<String> nameSet = java.util.Set.of(names);
        return results.stream()
                .filter(ResolvedSymbolReference::isResolved)
                .filter(r -> {
                    TextRange range = r.originalReference().range();
                    if (range.startOffset() < 0 || range.endOffset() > source.length()) {
                        return false;
                    }
                    String text = source.substring(range.startOffset(), range.endOffset());
                    return nameSet.contains(text);
                })
                .toList();
    }

    // 1. resolve local variable -------------------------------------------------
    @Test
    void resolveLocalVariable() {
        String source = """
                class A {
                    void test() {
                        int local = 1;
                        int other = local;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> localRefs = resolvedWithNames(results, source, "local");
        // The parser emits a NAME_EXPRESSION only for the *use* of `local` on
        // the RHS of the second declaration; the declaration name itself isn't
        // modelled as a NAME_EXPRESSION leaf (it's consumed by parseLocalVariableDeclaration).
        // So we expect at least one resolved reference whose symbol id names a LOCAL_VARIABLE.
        assertFalse(localRefs.isEmpty(), "expected a resolved `local` use");
        assertTrue(localRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.LOCAL_VARIABLE),
                "the resolved `local` use must point at a LOCAL_VARIABLE");
    }

    // 2. resolve parameter -------------------------------------------------
    @Test
    void resolveParameter() {
        String source = """
                class A {
                    void test(int parameter) {
                        int x = parameter;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> paramRefs = resolvedWithNames(results, source, "parameter");
        assertFalse(paramRefs.isEmpty(), "expected a resolved `parameter` use");
        assertTrue(paramRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.PARAMETER),
                "the resolved `parameter` use must point at a PARAMETER");
    }

    // 3. resolve field -------------------------------------------------
    @Test
    void resolveField() {
        String source = """
                class A {
                    int value;
                    void test() {
                        int x = value;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> valueRefs = resolvedWithNames(results, source, "value");
        assertFalse(valueRefs.isEmpty(), "expected a resolved `value` use");
        assertTrue(valueRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.FIELD),
                "the resolved `value` use must point at a FIELD");
    }

    // 4. unresolved identifier -------------------------------------------------
    @Test
    void unresolvedIdentifier() {
        String source = """
                class A {
                    void test() {
                        int x = zzz;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> unresolvedZzz = results.stream()
                .filter(ResolvedSymbolReference::isUnresolved)
                .filter(r -> source.substring(r.originalReference().range().startOffset(),
                        r.originalReference().range().endOffset()).equals("zzz"))
                .toList();
        assertFalse(unresolvedZzz.isEmpty(), "unresolved `zzz` should be reported");
    }

    // 5. inner scope shadows outer scope -------------------------------------------------
    @Test
    void innerScopeShadowsOuter() {
        String source = """
                class A {
                    int value;
                    void test() {
                        int value = 10;
                        value++;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> valueRefs = resolvedWithNames(results, source, "value");
        // The inner `value++` should resolve to LOCAL_VARIABLE, NOT to FIELD.
        assertFalse(valueRefs.isEmpty(), "expected at least one resolved `value`");
        List<ResolvedSymbolReference> innerUse = valueRefs.stream()
                .filter(r -> r.resolvedSymbolId().kind() == SymbolKind.LOCAL_VARIABLE)
                .toList();
        assertFalse(innerUse.isEmpty(),
                "inner `value` use should resolve to LOCAL_VARIABLE (shadow), got " +
                        valueRefs.stream().map(r -> r.resolvedSymbolId().kind().toString()).toList());
    }

    // 6. nested block lookup -------------------------------------------------
    @Test
    void nestedBlockLookup() {
        String source = """
                class A {
                    void test(int parameter) {
                        {
                            int x = parameter;
                        }
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> paramRefs = resolvedWithNames(results, source, "parameter");
        assertFalse(paramRefs.isEmpty(), "inner-block `parameter` use should resolve via BLOCK -> METHOD -> TYPE");
        assertTrue(paramRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.PARAMETER));
    }

    // 7. method scope sees type members -------------------------------------------------
    @Test
    void methodScopeSeesTypeMembers() {
        String source = """
                class A {
                    int field;
                    void test() {
                        int x = field;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> fieldRefs = resolvedWithNames(results, source, "field");
        assertFalse(fieldRefs.isEmpty(), "field should resolve from inside method body via TYPE chain");
        assertTrue(fieldRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.FIELD));
    }

    // 8. resolution result is immutable -------------------------------------------------
    @Test
    void resolutionResultIsImmutable() {
        String source = "class A { void test() { int x = 1; } }";
        List<ResolvedSymbolReference> results = resolve(source);
        assertNotNull(results);
        SymbolReference stub = new SymbolReference(
                SymbolId.of(0, 0, 1, SymbolKind.TYPE),
                TextRange.of(0, 1),
                SymbolReferenceKind.SIMPLE);
        assertThrows(UnsupportedOperationException.class, () -> results.add(ResolvedSymbolReference.unresolved(stub)));
    }

    // Bonus: contract null-input handling -------------------------------------------------
    @Test
    void nullArgumentsRejected() {
        JavaNameResolver resolver = new JavaNameResolver();
        String src = "class A {}";
        JavaLexerService s = new JavaLexerService();
        JavaTokenStream st = new JavaTokenStream(
                s.lex(DocumentSnapshot.oneShot(src)).tokens(), src);
        AstNode root = new JavaParser(st).parse().getAstRoot();
        ParserSnapshot snap = new ParserSnapshot(1, src, root);
        assertThrows(NullPointerException.class, () -> resolver.resolve(null, new ProjectSymbolTable()));
        assertThrows(NullPointerException.class, () -> resolver.resolve(snap, null));
    }
}
