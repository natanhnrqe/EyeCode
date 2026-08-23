package com.eyecode.runtime;

import java.util.List;

public record ResolvedExecution(Kind kind, List<List<String>> commands, String mainClass) {
    public enum Kind { STANDARD_JAVA, MAVEN, GRADLE, SPRING_MAVEN, SPRING_GRADLE }

    public ResolvedExecution {
        commands = commands.stream().map(List::copyOf).toList();
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("At least one execution command is required");
        }
    }
}
