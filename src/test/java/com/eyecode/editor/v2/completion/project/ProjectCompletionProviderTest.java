package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.java.lexer.JavaLexer;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SemanticResolver;
import com.eyecode.editor.v2.language.java.symbols.SymbolBuilder;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectCompletionProviderTest {

    private static final Path SRC = Path.of("test/Source.java");

    private JavaFileModel parse(String source) {
        JavaLexer lexer = new JavaLexer();
        JavaTokenStream stream = new JavaTokenStream(lexer.tokenize(source));
        JavaParser parser = new JavaParser(stream);
        return parser.parse();
    }

    private ProjectSymbolIndex indexSource(String source) {
        JavaFileModel model = parse(source);
        SymbolBuilder builder = new SymbolBuilder();
        List<ProjectSymbol> symbols = builder.build(model, SRC);
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        index.addAll(symbols);
        return index;
    }

    private LanguageContext contextAtMarker(String source, String marker) {
        int markerLine = -1;
        int markerCol = -1;
        String[] lines = source.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int idx = lines[i].indexOf(marker);
            if (idx >= 0) {
                markerLine = i;
                markerCol = idx;
                break;
            }
        }
        String cleanSource = source.replace(marker, "");
        EditorDocument doc = new EditorDocument(SRC, cleanSource);
        EditorPosition caret = new EditorPosition(markerLine, markerCol);
        EditorSelection selection = new EditorSelection(caret, caret);
        SyntaxSnapshot syntax = new SyntaxSnapshot(List.of());
        DiagnosticSnapshot diagnostics = DiagnosticSnapshot.empty();
        return new LanguageContext(doc, caret, selection, syntax, diagnostics);
    }

    private List<String> labels(CompletionSnapshot snapshot) {
        return snapshot.getItems().stream()
                .map(CompletionItem::getLabel)
                .toList();
    }

    // ── Test: variable receiver ───────────────────────────────────────────

    @Test
    void variableReceiverReturnsProgressionMembers() {
        String source = """
            class Progression {
                List<String> items;
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Dog extends Animal {
                void test() {
                    Progression prog;
                    prog.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Should include printProgression, got: " + items);
        assertTrue(items.contains("first"),
                "Should include first, got: " + items);
        assertTrue(items.contains("cur"),
                "Should include cur, got: " + items);
    }

    @Test
    void variableReceiverRespectsPrefixFiltering() {
        String source = """
            class Progression {
                List<String> items;
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Dog extends Animal {
                void test() {
                    Progression prog;
                    prog.pr||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Should include printProgression when prefix 'pr', got: " + items);
        assertFalse(items.contains("first"),
                "Should NOT include first when prefix 'pr', got: " + items);
    }

    // ── Test: this. ───────────────────────────────────────────────────────

    @Test
    void thisReceiverReturnsCurrentClassMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Dog extends Animal {
                void bark() {}
                void test() {
                    this.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("bark"),
                "this. should include current class method 'bark', got: " + items);
        assertTrue(items.contains("test"),
                "this. should include current class method 'test', got: " + items);
    }

    // ── Test: super. ──────────────────────────────────────────────────────

    @Test
    void superReceiverReturnsSuperclassMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Dog extends Animal {
                void bark() {}
                void test() {
                    super.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("makeSound"),
                "super. should include superclass method 'makeSound', got: " + items);
        assertTrue(items.contains("age"),
                "super. should include superclass field 'age', got: " + items);
    }

    // ── Test: parameter receiver ──────────────────────────────────────────

    @Test
    void parameterReceiverReturnsTypeMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Dog extends Animal {
                void execute(Progression prog) {
                    prog.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Parameter receiver should return Progression members, got: " + items);
        assertTrue(items.contains("first"),
                "Parameter receiver should include 'first', got: " + items);
        assertTrue(items.contains("cur"),
                "Parameter receiver should include 'cur', got: " + items);
    }

    // ── Test: field receiver ──────────────────────────────────────────────

    @Test
    void fieldReceiverReturnsFieldTypeMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Container {
                Progression progField;
                void test() {
                    progField.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Field receiver should return Progression members, got: " + items);
        assertTrue(items.contains("first"),
                "Field receiver should include 'first', got: " + items);
        assertTrue(items.contains("cur"),
                "Field receiver should include 'cur', got: " + items);
    }

    // ── Test: object (local variable) receiver ────────────────────────────

    @Test
    void objectReceiverReturnsTypeMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Runner {
                void run() {
                    Progression prog = new Progression();
                    prog.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Object receiver should return Progression members, got: " + items);
        assertTrue(items.contains("first"),
                "Object receiver should include 'first', got: " + items);
        assertTrue(items.contains("cur"),
                "Object receiver should include 'cur', got: " + items);
    }

    // ── Test: non-dot completion ──────────────────────────────────────────

    @Test
    void nonDotCompletionReturnsVisibleSymbols() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Test {
                void run() {
                    Progression prog;
                    pr||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("prog"),
                "Non-dot completion should include local variable 'prog', got: " + items);
        assertTrue(items.contains("printProgression"),
                "Non-dot completion should include 'printProgression', got: " + items);
    }

    // ── Test: no receiver when no dot before caret ────────────────────────

    @Test
    void noReceiverWhenCaretBeforeWord() {
        String source = """
            class Progression {
                List<String> items;
                void printProgression() {}
                String first;
                int cur;
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        // Without dot, should return visible symbols (not member completions)
        List<String> items = labels(result);
        assertTrue(items.contains("first"),
                "Without dot, should return visible symbols like 'first', got: " + items);
    }

    // ── Test: resolveCurrentClass returns null for null sourceFile ────────

    @Test
    void resolveCurrentClassReturnsEmptyForNullSourceFile() {
        String source = "class Foo { void test() { this.||CARET|| } }";
        String cleanSource = source.replace("||CARET||", "");

        EditorDocument doc = new EditorDocument(null, cleanSource);
        EditorPosition caret = new EditorPosition(0, source.indexOf("||CARET||"));
        EditorSelection selection = new EditorSelection(caret, caret);
        SyntaxSnapshot syntax = new SyntaxSnapshot(List.of());
        DiagnosticSnapshot diagnostics = DiagnosticSnapshot.empty();
        LanguageContext ctx = new LanguageContext(doc, caret, selection, syntax, diagnostics);

        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        assertNotNull(result);
    }

    // ── Test: standard library symbols ────────────────────────────────────

    @Test
    void standardLibrarySymbolsAvailableInResolver() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        SemanticResolver resolver = index.resolver();
        assertNotNull(resolver.resolveClass("String"),
                "StandardLibrarySymbols should provide String class");
        assertNotNull(resolver.resolveClass("List"),
                "StandardLibrarySymbols should provide List class");
        assertNotNull(resolver.resolveClass("Integer"),
                "StandardLibrarySymbols should provide Integer class");
    }

    @Test
    void stringMembersResolvedThroughStandardLibrary() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        SemanticResolver resolver = index.resolver();
        ProjectSymbol stringClass = resolver.resolveClass("String");
        assertNotNull(stringClass);

        List<ProjectSymbol> members = resolver.resolveMembers(stringClass);
        assertFalse(members.isEmpty(), "String class should have members");

        boolean hasLength = members.stream().anyMatch(m -> "length".equals(m.getName()));
        assertTrue(hasLength, "String should have length() method, got: "
                + members.stream().map(ProjectSymbol::getName).toList());
    }

    // ── Test: empty prefix with receiver ──────────────────────────────────

    @Test
    void receiverWithEmptyPrefixReturnsAllMembers() {
        String source = """
            class Progression {
                void printProgression() {}
                String first;
                int cur;
            }
            class Animal {
                void makeSound() {}
                int age;
            }
            class Test {
                void run() {
                    Progression prog;
                    prog.||CARET||
                }
            }
            """;

        ProjectSymbolIndex index = indexSource(source);
        LanguageContext ctx = contextAtMarker(source, "||CARET||");
        ProjectCompletionProvider provider = new ProjectCompletionProvider(index);
        CompletionSnapshot result = provider.complete(ctx);

        List<String> items = labels(result);
        assertTrue(items.contains("printProgression"),
                "Empty prefix should return all members, got: " + items);
        assertTrue(items.contains("first"),
                "Empty prefix should return 'first', got: " + items);
        assertTrue(items.contains("cur"),
                "Empty prefix should return 'cur', got: " + items);
    }
}
