package com.eyecode.lessons.session;

import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.content.LessonInlineContent;
import com.eyecode.lessons.content.LessonInlineContentType;
import com.eyecode.lessons.content.LessonKind;
import com.eyecode.lessons.practice.PracticeValidator;
import com.eyecode.lessons.practice.PracticeVerificationStatus;
import com.eyecode.lessons.presentation.PresentationOperationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonSessionServiceTest {
    @Test void advancesAndClampsTheRepresentativeSession() {
        LessonSessionService service = new LessonSessionService(new LessonContentService());
        LessonSessionSnapshot first = service.start("java.fundamentals.variables.int");
        assertEquals("java.fundamentals.variables.int", first.lessonId());
        assertEquals(0, first.currentStepIndex());
        assertEquals(LessonSessionPhase.PRESENTATION, first.phase());
        assertTrue(first.practice() == null);
        assertEquals("tipos-primitivos", first.workspace().entryFileId());
        assertEquals("TiposPrimitivos.java", first.workspace().files().getFirst().name());
        LessonSessionSnapshot integersInt = service.next(first.sessionId());
        assertEquals(1, integersInt.currentStepIndex());
        assertEquals(0, integersInt.currentPresentationIndex());
        assertEquals("int", integersInt.presentation().id());
        LessonSessionSnapshot integersLong = service.next(first.sessionId());
        assertEquals(1, integersLong.currentStepIndex());
        assertEquals(1, integersLong.currentPresentationIndex());
        assertEquals("long", integersLong.presentation().id());
        LessonSessionSnapshot practice = service.next(first.sessionId());
        assertEquals(LessonSessionPhase.PRACTICE, practice.phase());
        assertEquals("integer-score", practice.practice().id());
        assertEquals(List.of(LessonInlineContentType.TEXT, LessonInlineContentType.CODE,
                        LessonInlineContentType.TEXT, LessonInlineContentType.CODE, LessonInlineContentType.TEXT,
                        LessonInlineContentType.CODE, LessonInlineContentType.TEXT, LessonInlineContentType.CODE,
                        LessonInlineContentType.TEXT),
                practice.practice().instruction().stream().map(LessonInlineContent::type).toList());
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n\n    }\n}\n", practice.practice().starterCode());
        assertEquals(false, practice.canNext());
        assertEquals(LessonSessionPhase.PRACTICE, service.next(first.sessionId()).phase());
        assertEquals("long", service.previous(first.sessionId()).presentation().id());
        practice = service.next(first.sessionId());
        LessonSessionSnapshot completed = service.completePractice(first.sessionId());
        assertEquals(LessonSessionPhase.PRACTICE, completed.phase());
        assertTrue(completed.practiceCompleted());
        assertTrue(completed.canNext());
        LessonSessionSnapshot decimalsFloat = service.next(first.sessionId());
        assertEquals(2, decimalsFloat.currentStepIndex());
        assertEquals("float", decimalsFloat.presentation().id());
        assertTrue(decimalsFloat.practice() == null);
        assertEquals("tipos-primitivos", decimalsFloat.workspace().entryFileId());
        assertEquals("long", service.previous(first.sessionId()).presentation().id());
    }

    @Test void verifiesPracticeWithoutAdvancingUntilNext() {
        LessonSessionService service = new LessonSessionService(new LessonContentService());
        LessonSessionSnapshot first = service.start("java.fundamentals.variables.int");
        service.next(first.sessionId());
        service.next(first.sessionId());
        service.next(first.sessionId());

        var failed = service.verifyPractice(first.sessionId(), "integer-score", source("int score = 99;"), new PracticeValidator());
        assertEquals(PracticeVerificationStatus.WRONG_INITIALIZER, failed.verification().status());
        assertEquals(LessonSessionPhase.PRACTICE, failed.session().phase());
        assertFalse(failed.session().practiceCompleted());
        assertFalse(failed.session().canNext());

        var succeeded = service.verifyPractice(first.sessionId(), "integer-score", source("int score = 100;"), new PracticeValidator());
        assertEquals(PracticeVerificationStatus.SUCCESS, succeeded.verification().status());
        assertEquals(LessonSessionPhase.PRACTICE, succeeded.session().phase());
        assertTrue(succeeded.session().practiceCompleted());
        assertTrue(succeeded.session().canNext());
        assertEquals("float", service.next(first.sessionId()).presentation().id());
    }

    @Test void closePreventsFurtherNavigation() {
        LessonSessionService service = new LessonSessionService(new LessonContentService());
        LessonSessionSnapshot session = service.start("java.fundamentals.variables.int");
        assertEquals(LessonSessionState.CLOSED, service.close(session.sessionId()).state());
        assertThrows(IllegalArgumentException.class, () -> service.next(session.sessionId()));
    }

    @Test void startsTheoryAsAContentOnlyLesson() {
        LessonSessionSnapshot theory = new LessonSessionService(new LessonContentService()).start("java.fundamentals.jvm-jre-jdk");
        assertEquals(LessonKind.THEORY, theory.kind());
        assertEquals(LessonSessionPhase.PRESENTATION, theory.phase());
        assertTrue(theory.presentation().commands().isEmpty());
        assertTrue(theory.practice() == null);
        assertFalse(theory.canNext());
    }

    @Test void advancesTheFirstProgramFromPresentationThroughBothPractices() {
        LessonSessionService service = new LessonSessionService(new LessonContentService());
        LessonSessionSnapshot session = service.start("java.fundamentals.first-program");
        assertEquals(LessonSessionPhase.PRESENTATION, session.phase());
        assertEquals("program-overview", session.presentation().id());
        for (int index = 0; index < 4; index++) session = service.next(session.sessionId());
        session = service.next(session.sessionId());
        assertEquals(LessonSessionPhase.PRACTICE, session.phase());
        assertEquals("first-program-message", session.practice().id());
        assertFalse(session.canNext());
        session = service.verifyPractice(session.sessionId(), "first-program-message", firstProgramSource("Olá, EyeCode!"), new PracticeValidator()).session();
        assertTrue(session.practiceCompleted());
        session = service.next(session.sessionId());
        assertEquals("second-instruction", session.presentation().id());
        session = service.next(session.sessionId());
        assertEquals(LessonSessionPhase.PRACTICE, session.phase());
        assertEquals("first-program-second-line", session.practice().id());
        session = service.verifyPractice(session.sessionId(), "first-program-second-line", firstProgramSource("Olá, EyeCode!", "Meu primeiro programa Java!"), new PracticeValidator()).session();
        assertTrue(session.practiceCompleted());
    }

    @Test void previousProvidesTheSameCompilerProgramInReverse() {
        LessonSessionService service = new LessonSessionService(new LessonContentService());
        LessonSessionSnapshot first = service.start("java.fundamentals.comparison");
        LessonSessionSnapshot age = service.next(first.sessionId());
        LessonSessionSnapshot booleanState = service.next(first.sessionId());
        LessonSessionSnapshot back = service.previous(first.sessionId());

        assertEquals(LessonNavigationDirection.BACKWARD, back.navigationDirection());
        assertEquals(age.presentation().canonicalCode(), back.transitionProgram().targetCode());
        assertEquals(booleanState.presentation().canonicalCode(), back.transitionProgram().sourceCode());
        assertEquals(PresentationOperationType.DELETE_TEXT, back.transitionProgram().operations().getFirst().type());
    }

    private static String source(String declaration) {
        return "public class Main {\n    public static void main(String[] args) {\n        "
                + declaration + "\n    }\n}\n";
    }

    private static String firstProgramSource(String... lines) {
        String body = java.util.Arrays.stream(lines)
                .map(line -> "        System.out.println(\"" + line + "\");")
                .collect(java.util.stream.Collectors.joining("\n"));
        return "public class Main {\n    public static void main(String[] args) {\n" + body + "\n    }\n}\n";
    }
}
