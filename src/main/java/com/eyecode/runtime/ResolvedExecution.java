package com.eyecode.runtime;

import java.util.List;

public record ResolvedExecution(Kind kind, List<List<String>> commands, String mainClass,
                                List<RunPhase> commandPhases, Runnable cleanup) {
    public enum Kind { STANDARD_JAVA, MAVEN_JAVA_APPLICATION, MAVEN, GRADLE, SPRING_MAVEN, SPRING_GRADLE }

    public ResolvedExecution(Kind kind, List<List<String>> commands, String mainClass) {
        this(kind, commands, mainClass, defaultPhases(kind, commands), () -> { });
    }

    public ResolvedExecution(Kind kind, List<List<String>> commands, String mainClass, List<RunPhase> commandPhases) {
        this(kind, commands, mainClass, commandPhases, () -> { });
    }

    public ResolvedExecution {
        commands = commands.stream().map(List::copyOf).toList();
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("At least one execution command is required");
        }
        commandPhases = commandPhases == null ? defaultPhases(kind, commands) : List.copyOf(commandPhases);
        if (commandPhases.size() != commands.size()) {
            throw new IllegalArgumentException("Each execution command must have a run phase");
        }
        cleanup = cleanup == null ? () -> { } : cleanup;
    }

    private static List<RunPhase> defaultPhases(Kind kind, List<List<String>> commands) {
        if (kind == Kind.STANDARD_JAVA || kind == Kind.MAVEN_JAVA_APPLICATION) {
            if (commands.size() == 2) return List.of(RunPhase.COMPILING, RunPhase.RUNNING);
        }
        return java.util.Collections.nCopies(commands.size(), RunPhase.RUNNING);
    }
}
