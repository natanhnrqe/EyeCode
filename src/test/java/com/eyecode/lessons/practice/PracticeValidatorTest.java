package com.eyecode.lessons.practice;

import com.eyecode.lessons.content.LessonPractice;
import com.eyecode.lessons.content.LessonInlineContent;
import com.eyecode.lessons.content.LessonInlineContentType;
import com.eyecode.lessons.content.LessonContentService;
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
        assertEquals("Correto. Você concluiu a tarefa: Instrucao", result.message());
    }

    @Test void verifiesTheFirstProgramMessageStructurallyInsideMain() {
        LessonPractice message = new LessonPractice("first-program-message",
                List.of(new LessonInlineContent(LessonInlineContentType.TEXT, "Instrucao", null)), "class Main {}");
        PracticeVerificationResult success = validator.verify(message, firstProgram("Olá, EyeCode!"));
        assertEquals(PracticeVerificationStatus.SUCCESS, success.status());
        assertTrue(success.message().contains("Instrucao"));
        assertTrue(!success.message().contains("score") && !success.message().contains("100"));
        assertEquals(PracticeVerificationStatus.WRONG_OUTPUT, validator.verify(message, firstProgram("Olá, mundo!")).status());
        assertEquals(PracticeVerificationStatus.MISSING_OUTPUT, validator.verify(message, source("int count = 1;")).status());
    }

    @Test void verifiesTheSecondProgramLineInSequence() {
        LessonPractice secondLine = new LessonPractice("first-program-second-line",
                List.of(new LessonInlineContent(LessonInlineContentType.TEXT, "Instrucao", null)), "class Main {}");
        PracticeVerificationResult success = validator.verify(secondLine,
                firstProgram("Olá, EyeCode!", "Meu primeiro programa Java!"));
        assertEquals(PracticeVerificationStatus.SUCCESS, success.status());
        assertTrue(success.message().contains("Instrucao"));
        assertEquals(PracticeVerificationStatus.MISSING_SECOND_OUTPUT, validator.verify(secondLine,
                firstProgram("Olá, EyeCode!")).status());
        assertEquals(PracticeVerificationStatus.WRONG_OUTPUT, validator.verify(secondLine,
                firstProgram("Meu primeiro programa Java!", "Olá, EyeCode!")).status());
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

    private static String firstProgram(String... lines) {
        String body = java.util.Arrays.stream(lines)
                .map(line -> "        System.out.println(\"" + line + "\");")
                .collect(java.util.stream.Collectors.joining("\n"));
        return "public class Main {\n    public static void main(String[] args) {\n" + body + "\n    }\n}\n";
    }

    @Test void verifiesTheNewFundamentalsPractices() {
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("variables-declare-score"), source("int score = 10;")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("variables-change-score"), source("int score = 10; score = 25;")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("variables-print-score"), source("int score = 25; System.out.println(score);")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("strings-text"), source("char letra = 'B'; String nome = \"Bia\"; System.out.println(nome);")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("arithmetic-calculation"), source("int total = 8 + 2 - 3 * 4 / 2 % 3;")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("assignment-update"), source("int pontos = 10; pontos += 10; pontos++; pontos -= 1; pontos--;")).status());
        assertEquals(PracticeVerificationStatus.SUCCESS, validator.verify(practice("comparison-logic"), source("int nota = 70; boolean ativo = true; boolean aprovado = nota >= 60 && ativo;")).status());
    }

    @Test void keepsFeedbackBoundToThePracticeThatWasValidated() {
        LessonContentService content = new LessonContentService();
        LessonPractice score = content.load("java.fundamentals.variables").steps().getFirst().practice();
        LessonPractice strings = content.load("java.fundamentals.strings").steps().getLast().practice();
        LessonPractice firstProgram = content.load("java.fundamentals.first-program").steps().get(4).practice();

        PracticeVerificationResult scoreResult = validator.verify(score, source("int score = 10;"));
        PracticeVerificationResult stringsResult = validator.verify(strings, source("char letra = 'B'; String nome = \"Bia\"; System.out.println(nome);"));
        PracticeVerificationResult firstProgramResult = validator.verify(firstProgram, firstProgram("Olá, EyeCode!"));

        assertTrue(scoreResult.message().contains("score"));
        assertTrue(stringsResult.successful());
        assertTrue(stringsResult.message().contains("letra"));
        assertTrue(!stringsResult.message().contains("score") && !stringsResult.message().contains("100"));
        assertTrue(!firstProgramResult.message().contains("score") && !firstProgramResult.message().contains("100"));
    }

    private static LessonPractice practice(String id) {
        return new LessonPractice(id, List.of(new LessonInlineContent(LessonInlineContentType.TEXT, "Instrucao", null)), "class Main {}");
    }
}
