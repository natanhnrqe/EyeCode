package com.eyecode.lessons.session;

import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.content.LessonPractice;
import com.eyecode.lessons.practice.PracticeValidator;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LessonSessionService {
    private final LessonContentService contentService;
    private final Map<String, LessonSession> sessions = new LinkedHashMap<>();
    private String activeSessionId;

    public LessonSessionService(LessonContentService contentService) {
        this.contentService = contentService;
    }

    public synchronized LessonSessionSnapshot start(String lessonId) {
        if (activeSessionId != null) close(activeSessionId);
        LessonSession session = new LessonSession(contentService.load(lessonId));
        sessions.put(session.sessionId(), session);
        activeSessionId = session.sessionId();
        return snapshot(session);
    }

    public synchronized LessonSessionSnapshot next(String sessionId) {
        LessonSession session = active(sessionId);
        session.next();
        return snapshot(session);
    }

    public synchronized LessonSessionSnapshot previous(String sessionId) {
        LessonSession session = active(sessionId);
        session.previous();
        return snapshot(session);
    }
    public synchronized LessonSessionSnapshot completePractice(String sessionId) {
        LessonSession session = active(sessionId);
        session.completePractice();
        return snapshot(session);
    }

    public synchronized LessonPracticeVerification verifyPractice(String sessionId, String practiceId,
                                                                  String source, PracticeValidator validator) {
        LessonSession session = active(sessionId);
        if (session.phase() != LessonSessionPhase.PRACTICE) {
            throw new IllegalStateException("A prática não está ativa");
        }
        LessonPractice practice = session.content().steps().get(session.currentStepIndex()).practice();
        if (practice == null || !practice.id().equals(practiceId)) {
            throw new IllegalArgumentException("Prática de aula inválida");
        }
        var verification = validator.verify(practice, source);
        if (verification.successful()) session.completePractice();
        return new LessonPracticeVerification(snapshot(session), verification);
    }

    public synchronized LessonSessionSnapshot close(String sessionId) {
        LessonSession session = sessions.get(sessionId);
        if (session == null) throw new IllegalArgumentException("Sessão de aula inválida");
        session.close();
        if (sessionId.equals(activeSessionId)) activeSessionId = null;
        return snapshot(session);
    }

    public synchronized void closeActive() {
        if (activeSessionId != null) close(activeSessionId);
    }

    private LessonSession active(String sessionId) {
        LessonSession session = sessions.get(sessionId);
        if (session == null || !sessionId.equals(activeSessionId)) throw new IllegalArgumentException("Sessão de aula inválida");
        if (session.state() != LessonSessionState.ACTIVE) throw new IllegalStateException("A sessão de aula está encerrada");
        return session;
    }

    private static LessonSessionSnapshot snapshot(LessonSession session) {
        int index = session.currentStepIndex();
        int total = session.content().steps().size();
        var step = session.content().steps().get(index);
        int presentationIndex = session.currentPresentationIndex();
        boolean canPrevious = index > 0 || presentationIndex > 0 || session.phase() != LessonSessionPhase.PRESENTATION;
        boolean canNext = session.phase() == LessonSessionPhase.PRACTICE ? session.practiceCompleted()
                : index < total - 1 || presentationIndex < step.presentations().size() - 1 || step.practice() != null;
        return new LessonSessionSnapshot(session.sessionId(), session.content().id(), session.content().kind(), index, total, presentationIndex,
                session.state(), step, step.presentations().get(presentationIndex), step.practice(), session.phase(),
                session.practiceCompleted(), canPrevious, canNext);
    }
}
