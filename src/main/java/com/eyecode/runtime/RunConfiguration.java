package com.eyecode.runtime;

import java.nio.file.Path;

public record RunConfiguration(
        String id,
        String displayName,
        RunConfigurationKind kind,
        Path projectRoot,
        String mainClass
) {
    public RunConfiguration {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()
                || kind == null || projectRoot == null || mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("A complete run configuration is required");
        }
        projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    public String tooltip() {
        return mainClass + " (" + kind.name().replace('_', ' ') + ")";
    }
}
