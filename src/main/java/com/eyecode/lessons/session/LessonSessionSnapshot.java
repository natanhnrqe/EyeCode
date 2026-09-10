package com.eyecode.lessons.session;

import com.eyecode.lessons.content.LessonStep;
import com.eyecode.lessons.content.LessonPresentation;
import com.eyecode.lessons.content.LessonPractice;
import com.eyecode.lessons.content.LessonKind;

public record LessonSessionSnapshot(String sessionId, String lessonId, LessonKind kind, int currentStepIndex, int totalSteps,
                                    int currentPresentationIndex, LessonSessionState state, LessonStep step,
                                    LessonPresentation presentation, LessonPractice practice, LessonSessionPhase phase,
                                    boolean practiceCompleted, boolean canPrevious, boolean canNext) {
}
