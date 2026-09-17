package com.eyecode.lessons.content;

import org.junit.jupiter.api.Test;

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
        LessonEditorCommand skeleton = integers.presentations().getFirst().commands().getFirst();
        LessonEditorCommand animate = integers.presentations().getFirst().commands().get(2);
        LessonEditorCommand eraseForLong = integers.presentations().get(1).commands().get(1);
        LessonEditorCommand longInsert = integers.presentations().get(1).commands().get(2);
        LessonEditorCommand floatInsert = content.steps().get(2).presentations().getFirst().commands().get(2);
        LessonEditorCommand eraseForDouble = content.steps().get(2).presentations().get(1).commands().get(1);
        LessonEditorCommand doubleInsert = content.steps().get(2).presentations().get(1).commands().get(2);
        LessonEditorCommand charInsert = content.steps().get(3).presentations().getFirst().commands().get(2);
        LessonEditorCommand booleanInsert = content.steps().get(4).presentations().getFirst().commands().get(2);
        assertTrue(skeleton.code().contains("{\n    }"));
        assertEquals(LessonEditorCommandType.ANIMATE_EDIT, animate.type());
        assertFirstLineInsertion(animate, "int age = 20;");
        assertFirstLineInsertion(longInsert, "long population = 8_000_000_000L;");
        assertFirstLineInsertion(floatInsert, "float temperature = 36.5F;");
        assertFirstLineInsertion(doubleInsert, "double price = 19.99;");
        assertFirstLineInsertion(charInsert, "char grade = 'A';");
        assertFirstLineInsertion(booleanInsert, "boolean active = true;");
        assertEquals("", eraseForLong.replacementText());
        assertEquals(4, eraseForLong.range().startLineNumber());
        assertEquals(5, eraseForLong.range().endLineNumber());
        assertTrue(eraseForLong.finalCode().contains("{\n    }"));
        assertEquals(32, eraseForLong.cadenceMillis());
        assertEquals("", eraseForDouble.replacementText());
        assertEquals(4, eraseForDouble.range().startLineNumber());
        assertEquals(5, eraseForDouble.range().endLineNumber());
        assertEquals(32, eraseForDouble.cadenceMillis());
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n        int age = 20;\n    }\n}\n", animate.finalCode());
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n        long population = 8_000_000_000L;\n    }\n}\n", longInsert.finalCode());
        assertEquals(32, animate.cadenceMillis());
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
        assertEquals("strings-text", service.load("java.fundamentals.strings").steps().getFirst().practice().id());
        assertEquals(32, service.load("java.fundamentals.arithmetic").steps().getFirst().presentations().getFirst().commands().get(1).cadenceMillis());
        assertEquals("assignment-update", service.load("java.fundamentals.assignment").steps().getFirst().practice().id());
        assertEquals("comparison-logic", service.load("java.fundamentals.comparison").steps().getFirst().practice().id());
    }

    @Test void loadsTheFirstProgramWithTwoSequentialPractices() {
        LessonContent content = service.load("java.fundamentals.first-program");
        assertEquals(LessonKind.PRACTICE, content.kind());
        assertEquals(6, content.steps().size());
        String boilerplate = "public class Main {\n\n    public static void main(String[] args) {\n    }\n}\n";
        LessonEditorCommand initialCode = content.steps().get(0).presentations().getFirst().commands().getFirst();
        LessonEditorCommand printlnInsert = content.steps().get(2).presentations().getFirst().commands().get(2);
        LessonEditorCommand secondLineInsert = content.steps().get(3).presentations().getFirst().commands().get(2);
        assertEquals(boilerplate, initialCode.code());
        assertTrue(initialCode.code().contains("public static void main(String[] args)"));
        assertFalse(initialCode.code().contains("System.out.println"));
        assertEquals("\n        System.out.println(\"Olá, mundo!\");", printlnInsert.replacementText());
        assertEquals(3, printlnInsert.range().startLineNumber());
        assertEquals(45, printlnInsert.range().startColumn());
        assertEquals(printlnInsert.range().startLineNumber(), printlnInsert.range().endLineNumber());
        assertEquals(printlnInsert.range().startColumn(), printlnInsert.range().endColumn());
        assertTrue(printlnInsert.finalCode().contains("System.out.println(\"Olá, mundo!\");"));
        assertEquals("\n        System.out.println(\"Segunda linha\");", secondLineInsert.replacementText());
        assertTrue(secondLineInsert.finalCode().contains("System.out.println(\"Olá, mundo!\");\n        System.out.println(\"Segunda linha\");"));
        assertEquals(32, printlnInsert.cadenceMillis());
        assertEquals(32, secondLineInsert.cadenceMillis());
        assertEquals("first-program-message", content.steps().get(4).practice().id());
        assertEquals("first-program-second-line", content.steps().get(5).practice().id());
        assertTrue(content.workspace().files().getFirst().starterCode().contains("Olá, mundo!"));
        assertFalse(content.steps().get(4).contentBlocks().stream().filter(block -> block.type() == LessonContentBlockType.CODE)
                .anyMatch(block -> (block.code() == null ? "" : block.code()).contains("Olá, EyeCode!")));
        assertFalse(content.steps().get(5).contentBlocks().stream().filter(block -> block.type() == LessonContentBlockType.CODE)
                .anyMatch(block -> (block.code() == null ? "" : block.code()).contains("Meu primeiro programa Java!")));
        assertEquals(boilerplate, content.steps().get(2).presentations().getFirst().commands().getFirst().code());
        assertEquals(boilerplate.replace("    }\n}\n", "        System.out.println(\"Olá, mundo!\");\n    }\n}\n"),
                printlnInsert.finalCode());
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

    private static void assertFirstLineInsertion(LessonEditorCommand command, String source) {
        assertEquals("\n        " + source, command.replacementText());
        assertEquals(3, command.range().startLineNumber());
        assertEquals(45, command.range().startColumn());
        assertEquals(3, command.range().endLineNumber());
        assertEquals(45, command.range().endColumn());
    }
}
