package com.eyecode.lessons.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("", eraseForDouble.replacementText());
        assertEquals(4, eraseForDouble.range().startLineNumber());
        assertEquals(5, eraseForDouble.range().endLineNumber());
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n        int age = 20;\n    }\n}\n", animate.finalCode());
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n        long population = 8_000_000_000L;\n    }\n}\n", longInsert.finalCode());
        assertEquals(18, animate.cadenceMillis());
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
