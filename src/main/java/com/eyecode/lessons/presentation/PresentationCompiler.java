package com.eyecode.lessons.presentation;

/**
 * Compiles consecutive canonical lesson-code states into a presentation
 * program. Resources declare only canonical code and transition intent; the
 * compiler chooses operations and falls back to materialization whenever a
 * planned program cannot reproduce the exact canonical target.
 */
public final class PresentationCompiler {
    private final JavaPresentationChangeAnalyzer analyzer = new JavaPresentationChangeAnalyzer();
    private final PresentationTransitionPlanner planner = new PresentationTransitionPlanner();
    private final PresentationProgramExecutor executor = new PresentationProgramExecutor();

    public PresentationProgram compile(String previousCode, String canonicalCode, boolean instant) {
        if (previousCode == null || canonicalCode == null) throw new IllegalArgumentException("Estados canônicos ausentes");
        if (previousCode.isEmpty()) {
            return new PresentationProgram(previousCode, canonicalCode,
                    java.util.List.of(PresentationOperation.materialize(previousCode.length(), canonicalCode)));
        }
        CodeChange change = analyzer.analyze(previousCode, canonicalCode);
        PresentationProgram program = new PresentationProgram(previousCode, canonicalCode,
                planner.plan(previousCode, canonicalCode, change, instant));
        if (!executor.apply(program, previousCode).equals(canonicalCode)) {
            return new PresentationProgram(previousCode, canonicalCode,
                    java.util.List.of(PresentationOperation.materialize(previousCode.length(), canonicalCode)));
        }
        return program;
    }
}
