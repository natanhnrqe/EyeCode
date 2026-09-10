package com.eyecode.lessons.session;

import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.content.LessonKind;
import com.eyecode.lessons.practice.PracticeValidator;
import com.eyecode.lessons.practice.PracticeVerificationStatus;
import org.junit.jupiter.api.Test;

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
        assertEquals("Crie uma variável `int` chamada `score` com valor `100` dentro do método `main`.", practice.practice().instruction());
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

    private static String source(String declaration) {
        return "public class Main {\n    public static void main(String[] args) {\n        "
                + declaration + "\n    }\n}\n";
    }
}
