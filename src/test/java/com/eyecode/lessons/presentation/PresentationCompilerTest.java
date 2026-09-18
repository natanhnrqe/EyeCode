package com.eyecode.lessons.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationCompilerTest {
    private final PresentationCompiler compiler = new PresentationCompiler();
    private final PresentationProgramExecutor executor = new PresentationProgramExecutor();

    @Test void noChangeProducesAnEmptyProgram() {
        PresentationProgram program = compiler.compile("class Main {}", "class Main {}", false);
        assertTrue(program.operations().isEmpty());
        assertExact(program);
    }

    @Test void firstStateMaterializes() {
        PresentationProgram program = compiler.compile("", "class Main {}\n", false);
        assertEquals(PresentationOperationType.MATERIALIZE, program.operations().getFirst().type());
        assertExact(program);
    }

    @Test void localizedInsertDeleteAndReplaceUseMinimalOperations() {
        assertOperation("class A { int n = 1; }", "class A { int n = 10; }", PresentationOperationType.TYPE_TEXT);
        assertOperation("class A { int n = 10; }", "class A { int n = 1; }", PresentationOperationType.DELETE_TEXT);
        assertOperation("class A { int n = 1; }", "class A { int n = 2; }", PresentationOperationType.REPLACE_TEXT);
    }

    @Test void significantLineReplacementDeletesTheEntireOldLineBeforeTypingTheNewLine() {
        String before = "int idade = 20;";
        String after = "long populacao = 1000000;";
        PresentationProgram program = compiler.compile(before, after, false);
        PresentationOperation operation = program.operations().getFirst();
        assertEquals(PresentationOperationType.REPLACE_TEXT, operation.type());
        assertEquals(0, operation.startOffset());
        assertEquals(before.length(), operation.endOffset());
        assertEquals(after, operation.text());
        assertExact(program);
    }

    @Test void smallValueReplacementRemainsLocalized() {
        String before = "int idade = 20;";
        String after = "int idade = 21;";
        PresentationProgram program = compiler.compile(before, after, false);
        PresentationOperation operation = program.operations().getFirst();
        assertEquals(PresentationOperationType.REPLACE_TEXT, operation.type());
        assertTrue(operation.startOffset() > 0);
        assertTrue(operation.endOffset() < before.length());
        assertEquals("1", operation.text());
        assertExact(program);
    }

    @Test void reverseStatementTransitionUsesProgressiveDeletion() {
        String before = "class Main {\n    void run() {\n        int idade = 20;\n    }\n}\n";
        String after = "class Main {\n    void run() {\n        int idade = 20;\n        System.out.println(idade);\n    }\n}\n";
        PresentationProgram reverse = compiler.compile(after, before, false);
        assertEquals(PresentationOperationType.DELETE_TEXT, reverse.operations().getFirst().type());
        assertExact(reverse);
    }

    @Test void reverseLineReplacementDeletesTheNewLineBeforeTypingTheOldLine() {
        String before = "int idade = 20;";
        String after = "long populacao = 1000000;";
        PresentationProgram reverse = compiler.compile(after, before, false);
        PresentationOperation operation = reverse.operations().getFirst();
        assertEquals(PresentationOperationType.REPLACE_TEXT, operation.type());
        assertEquals(0, operation.startOffset());
        assertEquals(after.length(), operation.endOffset());
        assertEquals(before, operation.text());
        assertExact(reverse);
    }

    @Test void statementInsertionPreparesIndentationAndLineEndingOutsideTyping() {
        String before = "class Main {\n    void run() {\n    }\n}\n";
        String after = "class Main {\n    void run() {\n        int age = 20;\n    }\n}\n";
        PresentationProgram program = compiler.compile(before, after, false);
        PresentationOperation operation = program.operations().getFirst();
        assertEquals(PresentationOperationType.TYPE_TEXT, operation.type());
        assertEquals("        ", operation.prefix());
        assertEquals("int age = 20;", operation.text());
        assertEquals("\n    ", operation.suffix());
        assertExact(program);
    }

    @Test void crlfStatementInsertionPreservesExactLineEndings() {
        String before = "class Main {\r\n    void run() {\r\n    }\r\n}\r\n";
        String after = "class Main {\r\n    void run() {\r\n        int age = 20;\r\n    }\r\n}\r\n";
        PresentationProgram program = compiler.compile(before, after, false);
        assertEquals("\r\n    ", program.operations().getFirst().suffix());
        assertExact(program);
    }

    @Test void blockInsertionUsesAnAnimatedRegionReplacement() {
        String before = "class Main {\n    void run() {\n    }\n}\n";
        String after = "class Main {\n    void run() {\n        if (true) {\n            work();\n        }\n    }\n}\n";
        PresentationProgram program = compiler.compile(before, after, false);
        assertAnimated(program);
        assertEquals(PresentationOperationType.REPLACE_TEXT, program.operations().getFirst().type());
    }

    @Test void blockDeletionUsesAnAnimatedRegionReplacement() {
        String before = "class Main {\n    void run() {\n        if (true) {\n            work();\n        }\n    }\n}\n";
        String after = "class Main {\n    void run() {\n    }\n}\n";
        assertAnimated(compiler.compile(before, after, false));
    }

    @Test void multiHunkStructuralChangeUsesOneDeterministicAnimatedRegion() {
        String before = "class A {\n    int a = 1;\n    int b = 2;\n}\n";
        String after = "class Renamed {\n    int a = 1;\n    long b = 3;\n}\n";
        PresentationProgram program = compiler.compile(before, after, false);
        assertAnimated(program);
        assertEquals(PresentationOperationType.REPLACE_TEXT, program.operations().getFirst().type());
    }

    @Test void independentExamplesPreserveTheirSharedScaffoldDuringAnimatedReplacement() {
        String before = "public class Main {\n\n    public static void main(String[] args) {\n        int idade = 20;\n        System.out.println(idade);\n    }\n}\n";
        String after = "public class Main {\n\n    public static void main(String[] args) {\n        String nome = \"Ana\";\n        System.out.println(\"Olá, \" + nome);\n    }\n}\n";
        PresentationProgram forward = compiler.compile(before, after, false);
        PresentationProgram reverse = compiler.compile(after, before, false);
        assertAnimated(forward);
        assertAnimated(reverse);
        assertEquals(before.indexOf("        int idade"), forward.operations().getFirst().startOffset());
        assertEquals(before.indexOf("    }\n}"), forward.operations().getFirst().endOffset());
    }

    @Test void bracesInsideStringsAndCommentsDoNotPreventExactLocalizedChanges() {
        assertOperation("class A { String s = \"{old}\"; }", "class A { String s = \"{new}\"; }",
                PresentationOperationType.REPLACE_TEXT);
        assertOperation("class A { void x() { /* } old */ } }", "class A { void x() { /* } new */ } }",
                PresentationOperationType.REPLACE_TEXT);
    }

    @Test void explicitInstantFlagDoesNotSuppressARegularCanonicalAnimation() {
        PresentationProgram program = compiler.compile("class A {}", "class B {}", true);
        assertAnimated(program);
    }

    private void assertOperation(String before, String after, PresentationOperationType expected) {
        PresentationProgram program = compiler.compile(before, after, false);
        assertEquals(expected, program.operations().getFirst().type());
        assertExact(program);
    }

    private void assertExact(PresentationProgram program) {
        assertEquals(program.targetCode(), executor.apply(program, program.sourceCode()));
    }

    private void assertAnimated(PresentationProgram program) {
        assertTrue(program.isAnimated());
        assertExact(program);
    }
}
