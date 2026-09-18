package com.eyecode.lessons.presentation;

import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.content.LessonPresentation;
import com.eyecode.lessons.content.LessonStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundamentalsPresentationTransitionTest {
    private static final List<String> LESSONS = List.of("java.fundamentals.first-program", "java.fundamentals.variables",
            "java.fundamentals.variables.int", "java.fundamentals.strings", "java.fundamentals.arithmetic",
            "java.fundamentals.assignment", "java.fundamentals.comparison");

    private final LessonContentService content = new LessonContentService();
    private final PresentationCompiler compiler = new PresentationCompiler();
    private final PresentationProgramExecutor executor = new PresentationProgramExecutor();
    private final JavaPresentationChangeAnalyzer analyzer = new JavaPresentationChangeAnalyzer();

    @Test void auditsAndProvesEveryForwardAndReverseCanonicalTransition() {
        for (String lessonId : LESSONS) {
            List<PresentationRef> states = states(lessonId);
            String previous = "";
            String previousId = "início";
            for (PresentationRef current : states) {
                assertTransition(lessonId, previousId, current, previous, current.code());
                previous = current.code();
                previousId = current.stepId();
            }
            for (int index = 1; index < states.size(); index++) {
                PresentationRef source = states.get(index);
                PresentationRef target = states.get(index - 1);
                assertTransition(lessonId, source.stepId(), target, source.code(), target.code());
            }
        }
    }

    private void assertTransition(String lessonId, String sourceId, PresentationRef target,
                                  String source, String expected) {
        CodeChange change = analyzer.analyze(source, expected);
        PresentationProgram program = compiler.compile(source, expected, false);
        String operation = program.operations().isEmpty() ? "NO_OPERATION"
                : program.operations().getFirst().type().name();
        System.out.printf("[presentation-audit] %s %s -> %s %s %s%n",
                lessonId, sourceId, target.stepId(), change.kind(), operation);
        assertEquals(expected, executor.apply(program, source));
        if (!source.isEmpty() && !source.equals(expected)) assertTrue(program.isAnimated(),
                () -> lessonId + " " + sourceId + " -> " + target.stepId() + " não possui animação");
    }

    private List<PresentationRef> states(String lessonId) {
        return content.load(lessonId).steps().stream()
                .flatMap(step -> step.presentations().stream()
                        .filter(presentation -> presentation.canonicalCode() != null)
                        .map(presentation -> new PresentationRef(step.id() + "/" + presentation.id(), presentation)))
                .toList();
    }

    private record PresentationRef(String stepId, LessonPresentation presentation) {
        String code() {
            return presentation.canonicalCode();
        }
    }
}
