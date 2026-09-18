package com.eyecode.lessons.content;

import org.junit.jupiter.api.Test;
import com.eyecode.lessons.presentation.PresentationOperationType;
import com.eyecode.lessons.presentation.PresentationProgramExecutor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonContentServiceTest {
    private final LessonContentService service = new LessonContentService();

    @Test void loadsTheRepresentativeLesson() {
        LessonContent content = service.load("java.fundamentals.variables.int");
        assertEquals("java.fundamentals.variables.int", content.id());
        assertEquals(LessonKind.PRACTICE, content.kind());
        assertEquals(6, content.steps().size());
        assertEquals("Tipos Primitivos", content.title());
        assertEquals(LessonContentBlockType.HEADING, content.steps().get(1).contentBlocks().getFirst().type());
        LessonStep integers = content.steps().get(1);
        assertEquals("int", integers.presentations().getFirst().annotation().title());
        assertEquals("long", integers.presentations().get(1).annotation().title());
        assertTrue(integers.presentations().stream().allMatch(presentation -> presentation.program() != null));
        assertEquals(PresentationOperationType.TYPE_TEXT,
                integers.presentations().getFirst().program().operations().getFirst().type());
        assertEquals(PresentationOperationType.REPLACE_TEXT,
                integers.presentations().get(1).program().operations().getFirst().type());
        assertTrue(integers.presentations().stream().flatMap(presentation -> presentation.commands().stream())
                .allMatch(command -> command.type() == LessonEditorCommandType.HIGHLIGHT_RANGE
                        || command.type() == LessonEditorCommandType.REVEAL_RANGE
                        || command.type() == LessonEditorCommandType.CLEAR_HIGHLIGHTS));
    }

    @Test void loadsTheTheoryLessonWithoutProfessorCommands() {
        LessonContent content = service.load("java.fundamentals.jvm-jre-jdk");
        assertEquals(LessonKind.THEORY, content.kind());
        assertEquals("JVM, JRE e JDK", content.title());
        assertEquals(1, content.steps().size());
        assertTrue(content.steps().getFirst().presentations().getFirst().commands().isEmpty());
        assertTrue(content.steps().getFirst().practice() == null);
        assertTrue(content.steps().getFirst().contentBlocks().size() >= 10);
        assertTrue(content.steps().getFirst().contentBlocks().stream().anyMatch(block -> block.inlineContent().stream()
                .anyMatch(inline -> inline.type() == LessonInlineContentType.CODE)));
        assertTrue(content.steps().getFirst().contentBlocks().stream().anyMatch(block -> "java".equals(block.language())));
    }

    @Test void loadsTheHowJavaWorksTheoryAsPresentationOnly() {
        LessonContent content = service.load("java.fundamentals.how-java-works");
        assertEquals(LessonKind.THEORY, content.kind());
        assertEquals(5, content.steps().size());
        assertTrue(content.steps().stream().allMatch(step -> step.practice() == null));
        assertTrue(content.steps().stream().allMatch(step -> step.presentations().getFirst().commands().isEmpty()));
        assertTrue(content.steps().stream().flatMap(step -> step.contentBlocks().stream())
                .anyMatch(block -> "Main.java\n    ↓\njavac\n    ↓\nMain.class".equals(block.code())));
        assertTrue(content.steps().stream().flatMap(step -> step.contentBlocks().stream())
                .anyMatch(block -> block.text() != null && block.text().contains("JVM")));
    }

    @Test void loadsTheVariablesAndOperatorsBlockLessons() {
        assertEquals("Variáveis", service.load("java.fundamentals.variables").title());
        assertEquals(3, service.load("java.fundamentals.variables").steps().size());
        assertEquals("strings-text", service.load("java.fundamentals.strings").steps().getLast().practice().id());
        assertTrue(service.load("java.fundamentals.arithmetic").steps().getFirst().presentations().getFirst().program() != null);
        assertEquals("assignment-update", service.load("java.fundamentals.assignment").steps().getLast().practice().id());
        assertEquals("comparison-logic", service.load("java.fundamentals.comparison").steps().getLast().practice().id());
    }

    @Test void fundamentalsCodePresentationsUseCanonicalV2Transitions() {
        List<String> lessonIds = List.of("java.fundamentals.first-program", "java.fundamentals.variables",
                "java.fundamentals.variables.int", "java.fundamentals.strings", "java.fundamentals.arithmetic",
                "java.fundamentals.assignment", "java.fundamentals.comparison");
        for (String lessonId : lessonIds) {
            String previousCanonical = "";
            for (LessonStep step : service.load(lessonId).steps()) {
                for (LessonPresentation presentation : step.presentations()) {
                    assertTrue(presentation.commands().stream().allMatch(command ->
                            command.type() == LessonEditorCommandType.HIGHLIGHT_RANGE
                                    || command.type() == LessonEditorCommandType.REVEAL_RANGE
                                    || command.type() == LessonEditorCommandType.CLEAR_HIGHLIGHTS));
                    if (presentation.canonicalCode() == null) continue;
                    assertTrue(presentation.program() != null);
                    assertEquals(previousCanonical, presentation.program().sourceCode());
                    assertEquals(presentation.canonicalCode(), new PresentationProgramExecutor().apply(
                            presentation.program(), presentation.program().sourceCode()));
                    if (!previousCanonical.isEmpty() && !previousCanonical.equals(presentation.canonicalCode())) {
                        assertTrue(presentation.program().isAnimated(), lessonId + " contém uma transição canônica sem animação");
                    }
                    previousCanonical = presentation.canonicalCode();
                }
            }
        }
    }

    @Test void comparisonPresentationDeclaresCanonicalStatesAndStructuralAppends() {
        LessonContent content = service.load("java.fundamentals.comparison");
        assertEquals(5, content.steps().size());
        for (int index = 0; index < 4; index++) {
            LessonPresentation presentation = content.steps().get(index).presentations().getFirst();
            assertTrue(presentation.canonicalCode() != null);
            assertEquals(index == 0 ? PresentationOperationType.MATERIALIZE : PresentationOperationType.TYPE_TEXT,
                    presentation.program().operations().getFirst().type());
            assertEquals(presentation.canonicalCode(), presentation.program().targetCode());
        }
        assertTrue(content.steps().get(3).presentations().getFirst().canonicalCode()
                .contains("boolean podeEntrar = maiorDeIdade && possuiIngresso;\n    }"));
    }

    @Test void loadsTheFirstProgramWithTwoSequentialPractices() {
        LessonContent content = service.load("java.fundamentals.first-program");
        assertEquals(LessonKind.PRACTICE, content.kind());
        assertEquals(6, content.steps().size());
        String boilerplate = "public class Main {\n\n    public static void main(String[] args) {\n    }\n}\n";
        var initial = content.steps().get(0).presentations().getFirst();
        var printlnInsert = content.steps().get(2).presentations().getFirst();
        var secondLineInsert = content.steps().get(3).presentations().getFirst();
        assertEquals(boilerplate, initial.canonicalCode());
        assertEquals(PresentationOperationType.MATERIALIZE, initial.program().operations().getFirst().type());
        assertEquals(PresentationOperationType.TYPE_TEXT, printlnInsert.program().operations().getFirst().type());
        assertEquals(PresentationOperationType.TYPE_TEXT, secondLineInsert.program().operations().getFirst().type());
        assertEquals("first-program-message", content.steps().get(4).practice().id());
        assertEquals("first-program-second-line", content.steps().get(5).practice().id());
        assertTrue(content.workspace().files().getFirst().starterCode().contains("Olá, mundo!"));
        assertFalse(content.steps().get(4).contentBlocks().stream().filter(block -> block.type() == LessonContentBlockType.CODE)
                .anyMatch(block -> (block.code() == null ? "" : block.code()).contains("Olá, EyeCode!")));
        assertFalse(content.steps().get(5).contentBlocks().stream().filter(block -> block.type() == LessonContentBlockType.CODE)
                .anyMatch(block -> (block.code() == null ? "" : block.code()).contains("Meu primeiro programa Java!")));
        assertEquals(boilerplate.replace("    }\n}\n", "        System.out.println(\"Olá, mundo!\");\n    }\n}\n"),
                printlnInsert.canonicalCode());
    }

    @Test void parsesPracticeFileIdentityAndKeepsTheLegacyStarterCodeCompatible() {
        LessonPractice implicit = service.load("java.fundamentals.variables.int").steps().get(1).practice();
        assertEquals("TiposPrimitivos.java", implicit.file().name());

        String lesson = "{\"id\":\"lesson\",\"version\":1,\"kind\":\"PRACTICE\",\"title\":\"Aula\",\"steps\":[{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Passo\",\"message\":\"Texto\",\"presentations\":[{\"id\":\"one\",\"commands\":[]}],\"practice\":{\"id\":\"file\",\"instruction\":\"Edite `Main.java`\",\"file\":{\"id\":\"main\",\"name\":\"Main.java\",\"language\":\"java\",\"starterCode\":\"class Main {}\",\"readOnly\":false}}}]}";
        LessonPractice practice = service.parse(lesson, "lesson").steps().getFirst().practice();
        assertEquals("main", practice.file().id());
        assertEquals("Main.java", practice.file().name());
        assertTrue(!practice.file().readOnly());

        String multiFileLesson = "{\"id\":\"lesson\",\"version\":1,\"kind\":\"PRACTICE\",\"title\":\"Aula\",\"steps\":[{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Passo\",\"message\":\"Texto\",\"presentations\":[{\"id\":\"one\",\"commands\":[]}],\"practice\":{\"id\":\"files\",\"instruction\":\"Edite `Foo.java`\",\"files\":[{\"id\":\"foo\",\"name\":\"Foo.java\",\"language\":\"java\",\"starterCode\":\"class Foo {}\",\"readOnly\":false},{\"id\":\"bar\",\"name\":\"Bar.java\",\"language\":\"java\",\"starterCode\":\"class Bar {}\",\"readOnly\":false}],\"entryFileId\":\"bar\"}}]}";
        LessonPractice multiFilePractice = service.parse(multiFileLesson, "lesson").steps().getFirst().practice();
        assertEquals(List.of("Foo.java", "Bar.java"), multiFilePractice.files().stream().map(LessonFile::name).toList());
        assertEquals("bar", multiFilePractice.entryFileId());
        assertEquals("Bar.java", multiFilePractice.file().name());
    }

    @Test void rejectsUnknownMalformedAndInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> service.load("lesson.missing"));
        assertThrows(IllegalArgumentException.class, () -> service.parse("{}", "lesson"));
        String malformedRange = "{\"id\":\"lesson\",\"version\":1,\"title\":\"Aula\",\"steps\":[{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Passo\",\"message\":\"Texto\",\"presentations\":[{\"id\":\"one\",\"commands\":[{\"type\":\"HIGHLIGHT_RANGE\",\"range\":{\"startLineNumber\":1,\"startColumn\":4,\"endLineNumber\":1,\"endColumn\":2}}]}]}]}";
        assertThrows(IllegalArgumentException.class, () -> service.parse(malformedRange, "lesson"));
        String invalidBlock = "{\"id\":\"lesson\",\"version\":1,\"title\":\"Aula\",\"steps\":[{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Passo\",\"message\":\"Texto\",\"contentBlocks\":[{\"type\":\"CODE\"}],\"presentations\":[]}]}";
        assertThrows(IllegalArgumentException.class, () -> service.parse(invalidBlock, "lesson"));
        String invalidAnimation = "{\"id\":\"lesson\",\"version\":1,\"title\":\"Aula\",\"steps\":[{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Passo\",\"message\":\"Texto\",\"presentations\":[{\"id\":\"one\",\"commands\":[{\"type\":\"ANIMATE_EDIT\",\"replacementText\":\"x\",\"range\":{\"startLineNumber\":1,\"startColumn\":1,\"endLineNumber\":1,\"endColumn\":1},\"finalCode\":\"x\",\"cadenceMillis\":0}]}]}]}";
        assertThrows(IllegalArgumentException.class, () -> service.parse(invalidAnimation, "lesson"));
    }

    @Test void rejectsDuplicateStepIds() {
        String duplicate = "{\"id\":\"lesson\",\"version\":1,\"title\":\"Aula\",\"steps\":["
                + "{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Um\",\"message\":\"Texto\",\"commands\":[]},"
                + "{\"id\":\"one\",\"type\":\"DEMO\",\"title\":\"Dois\",\"message\":\"Texto\",\"commands\":[]}] }";
        assertThrows(IllegalArgumentException.class, () -> service.parse(duplicate, "lesson"));
    }

}
