package com.eyecode.runtime;

import java.util.List;

/**
 * Post-execution trace of a lesson program, captured from a real instrumented
 * run. Every value in {@code steps} was observed in the virtual machine while
 * the lesson program executed; the trace never estimates state.
 *
 * <p>Each step represents the state observed when execution reached a source
 * line, before that line runs. Consecutive steps therefore show the state
 * before and after each instruction.</p>
 */
public record LessonTrace(
        String mainClass,
        LessonTraceStatus status,
        int exitCode,
        String runtimeError,
        List<Step> steps,
        List<Chunk> output,
        List<String> limitations
) {
    public record Step(int index, String className, int line, List<Variable> variables) {
        public Step {
            variables = variables == null ? List.of() : List.copyOf(variables);
        }
    }

    /**
     * A captured value. {@code kind} is one of {@code primitive}, {@code string},
     * {@code char}, {@code boolean}, {@code array}, {@code object}, {@code null},
     * {@code reference} or {@code unsupported}. {@code children} holds array
     * elements (named {@code [i]}) or object fields.
     */
    public record Variable(String name, String type, String value, String kind,
                           List<Variable> children, boolean truncated) {
        public Variable {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    public record Chunk(String text, boolean error) {
    }

    public LessonTrace {
        steps = steps == null ? List.of() : List.copyOf(steps);
        output = output == null ? List.of() : List.copyOf(output);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
