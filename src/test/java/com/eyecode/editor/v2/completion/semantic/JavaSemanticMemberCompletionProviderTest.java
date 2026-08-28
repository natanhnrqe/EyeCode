package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSemanticMemberCompletionProviderTest {

    private final JavaSemanticMemberCompletionProvider provider = new JavaSemanticMemberCompletionProvider();

    @Test
    void resolvesJdkMembersFromTheDeclaredReceiverType() {
        assertMembers("""
                class Main {
                    void test() {
                        String text = "";
                        text.| 
                    }
                }
                """, "length", "substring");
        assertMembers("""
                class Main {
                    void test() {
                        StringBuilder builder = new StringBuilder();
                        builder.| 
                    }
                }
                """, "append", "reverse");
        assertMembers("""
                import java.util.List;
                class Main {
                    void test() {
                        List<String> names = List.of();
                        names.| 
                    }
                }
                """, "add", "size");
    }

    @Test
    void resolvesProjectMembersFromTheCurrentUnsavedSnapshot() {
        CompletionSnapshot snapshot = complete("""
                class Person {
                    String name;
                    int age;
                    String getName() { return name; }
                    void sayHello() { }
                }
                class Main {
                    void test() {
                        Person person = new Person();
                        person.| 
                    }
                }
                """);

        assertLabels(snapshot, "name", "age", "getName", "sayHello");
        assertFalse(labels(snapshot).contains("public"));
        assertFalse(labels(snapshot).contains("EyeCode"));
    }

    @Test
    void resolvesInheritedProjectMembers() {
        assertMembers("""
                class Animal { void eat() { } }
                class Dog extends Animal { void bark() { } }
                class Main {
                    void test() {
                        Dog dog = new Dog();
                        dog.| 
                    }
                }
                """, "bark", "eat");
    }

    @Test
    void filtersOnlyTheReceiverMembersForPartialMemberPrefixes() {
        CompletionSnapshot snapshot = complete("""
                class Person {
                    String getName() { return ""; }
                    void sayHello() { }
                }
                class Main {
                    void test() {
                        Person person = new Person();
                        person.get| 
                    }
                }
                """);

        assertTrue(labels(snapshot).contains("getName"));
        assertFalse(labels(snapshot).contains("sayHello"));
        assertFalse(labels(snapshot).contains("return"));
    }

    @Test
    void resolvesChainedJdkReceiverTypes() {
        assertMembers("""
                class Main {
                    void test() {
                        System.out.| 
                    }
                }
                """, "print", "println");
    }

    private CompletionSnapshot complete(String sourceWithCaret) {
        int offset = sourceWithCaret.indexOf('|');
        String source = sourceWithCaret.substring(0, offset) + sourceWithCaret.substring(offset + 1);
        EditorDocument document = new EditorDocument(null, source);
        EditorPosition position = document.positionOf(offset);
        LanguageContext context = new LanguageContext(document, position,
                new EditorSelection(position, position),
                new JavaSyntaxAnalyzer().analyze(document), DiagnosticSnapshot.empty());
        return provider.complete(context, true);
    }

    private void assertMembers(String sourceWithCaret, String... expected) {
        assertLabels(complete(sourceWithCaret), expected);
    }

    private void assertLabels(CompletionSnapshot snapshot, String... expected) {
        List<String> labels = labels(snapshot);
        for (String label : expected) {
            assertTrue(labels.contains(label), () -> "missing " + label + " in " + labels);
        }
    }

    private List<String> labels(CompletionSnapshot snapshot) {
        return snapshot.getItems().stream().map(CompletionItem::getLabel).toList();
    }
}
