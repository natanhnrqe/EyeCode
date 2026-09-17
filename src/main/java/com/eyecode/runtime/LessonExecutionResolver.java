package com.eyecode.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LessonExecutionResolver {
    public ResolvedExecution resolve(LessonRunRequest request) {
        try {
            Path root = Files.createTempDirectory("eyecode-lesson-run-");
            Path sources = root.resolve("src");
            Path output = root.resolve("classes");
            List<String> javaSources = new ArrayList<>();
            for (LessonRunRequest.SourceFile file : request.files()) {
                Path target = safeSourcePath(sources, file.name());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.source(), StandardCharsets.UTF_8);
                if (target.toString().endsWith(".java")) javaSources.add(target.toString());
            }
            if (javaSources.isEmpty()) throw new IllegalArgumentException("Lesson workspace has no Java source files");
            Files.createDirectories(output);
            List<String> compile = new ArrayList<>(List.of("javac", "-encoding", "UTF-8", "-d", output.toString()));
            compile.addAll(javaSources);
            return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                    List.of(compile, List.of("java", "-cp", output.toString(), request.mainClass())), request.mainClass(),
                    List.of(RunPhase.COMPILING, RunPhase.RUNNING), () -> deleteRecursively(root));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not materialize lesson workspace", exception);
        }
    }

    private static Path safeSourcePath(Path root, String name) {
        Path relative = Path.of(name).normalize();
        if (relative.isAbsolute() || relative.startsWith("..") || relative.getNameCount() == 0) {
            throw new IllegalArgumentException("Invalid lesson source path");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid lesson source path");
        return target;
    }

    private static void deleteRecursively(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
