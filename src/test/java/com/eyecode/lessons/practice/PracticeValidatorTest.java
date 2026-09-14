package com.eyecode.lessons.practice;

import com.eyecode.lessons.content.LessonPractice;
import com.eyecode.lessons.content.LessonInlineContent;
import com.eyecode.lessons.content.LessonInlineContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PracticeValidatorTest {
    private final PracticeValidator validator = new PracticeValidator();
    private final LessonPractice practice = new LessonPractice("integer-score",
            List.of(new LessonInlineContent(LessonInlineContentType.TEXT, "Instrucao", null)), "class Main {}");

    @Test void acceptsTheRequiredDeclaration() {
        assertStatus(PracticeVerificationStatus.SUCCESS, source("int score = 100;"));
    }

    @Test void acceptsWhitespaceAndComments() {
        assertStatus(PracticeVerificationStatus.SUCCESS, source("""
                /* pontuação */
                int   score /* valor */ = 100 ;
                """));
    }

    @Test void rejectsWrongInitializer() {
        assertStatus(PracticeVerificationStatus.WRONG_INITIALIZER, source("int score = 99;"));
    }

    @Test void rejectsWrongType() {
        assertStatus(PracticeVerificationStatus.WRONG_TYPE, source("long score = 100;"));
    }

    @Test void rejectsWrongName() {
        assertStatus(PracticeVerificationStatus.WRONG_NAME, source("int points = 100;"));
    }

    @Test void rejectsMissingDeclaration() {
        assertStatus(PracticeVerificationStatus.MISSING_DECLARATION, source("String name = \"EyeCode\";"));
    }

    @Test void rejectsDeclarationOutsideMain() {
        assertStatus(PracticeVerificationStatus.INVALID_CONTEXT, """
                public class Main {
                    int score = 100;
                    public static void main(String[] args) { }
                }
                """);
    }

    @Test void rejectsMissingMainContext() {
        assertStatus(PracticeVerificationStatus.INVALID_CONTEXT, """
                public class Main {
                    void run() { int score = 100; }
                }
                """);
    }

    @Test void rejectsMalformedJava() {
        assertStatus(PracticeVerificationStatus.SYNTAX_ERROR, "package ;");
    }

    @Test void ignoresUnrelatedDeclarations() {
        assertStatus(PracticeVerificationStatus.WRONG_INITIALIZER, source("""
                int score = 99;
                int total = 100;
                """));
    }

    @Test void reportsSuccessThroughTheStructuredResult() {
        PracticeVerificationResult result = validator.verify(practice, source("int score = 100;"));
        assertTrue(result.successful());
        assertEquals("Correto. Você declarou `score` como `int` e inicializou com `100`.", result.message());
    }

    private void assertStatus(PracticeVerificationStatus expected, String source) {
        assertEquals(expected, validator.verify(practice, source).status());
    }

    private static String source(String declaration) {
        return "public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "        " + declaration + "\n"
                + "    }\n"
                + "}\n";
    }
}
