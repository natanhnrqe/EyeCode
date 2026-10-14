package com.eyecode.runtime;

import java.nio.file.Path;
import java.util.Objects;

public record JavaToolchain(Path home, Path javaExecutable, Path javacExecutable) {
    public JavaToolchain {
        Objects.requireNonNull(home, "home");
        Objects.requireNonNull(javaExecutable, "javaExecutable");
        Objects.requireNonNull(javacExecutable, "javacExecutable");
    }

    public String javaHomeValue() {
        return home.toString();
    }

    public Path binDirectory() {
        return home.resolve("bin");
    }

    public boolean matches(Path candidateHome) {
        return home.equals(candidateHome);
    }
}
