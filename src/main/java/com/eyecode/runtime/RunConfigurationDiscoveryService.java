package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RunConfigurationDiscoveryService {
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:class|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern MAIN = Pattern.compile("\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\]\\s+[A-Za-z_$][\\w$]*\\s*\\)");
    private static final Pattern SPRING = Pattern.compile("@SpringBootApplication\\b");

    public List<RunConfiguration> discover(ProjectModel project) {
        if (project == null) {
            return List.of();
        }
        Path root = project.getRootDir().toAbsolutePath().normalize();
        List<Path> roots = sourceRoots(root);
        List<RunConfiguration> result = new ArrayList<>();
        for (Path sourceRoot : roots) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (var files = Files.walk(sourceRoot)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> inspect(path, root, result));
            } catch (IOException ignored) {
            }
        }
        result.sort(Comparator.comparing(RunConfiguration::id));
        return List.copyOf(result);
    }

    public Optional<RunConfiguration> defaultConfiguration(List<RunConfiguration> configurations) {
        if (configurations == null || configurations.isEmpty()) return Optional.empty();
        return configurations.stream()
                .filter(value -> value.kind() == RunConfigurationKind.SPRING_BOOT)
                .findFirst()
                .or(() -> configurations.stream().filter(value -> value.mainClass().endsWith(".Main")
                        || value.mainClass().equals("Main") || value.mainClass().endsWith(".Application")
                        || value.mainClass().equals("Application")).findFirst())
                .or(() -> configurations.stream().findFirst());
    }

    private void inspect(Path file, Path root, List<RunConfiguration> result) {
        String masked = mask(read(file));
        Matcher type = TYPE.matcher(masked);
        if (!type.find()) {
            return;
        }
        String packageName = packageName(masked);
        String mainClass = packageName.isBlank() ? type.group(1) : packageName + "." + type.group(1);
        boolean spring = SPRING.matcher(masked).find();
        boolean main = MAIN.matcher(masked).find();
        if (!spring && !main) {
            return;
        }
        RunConfigurationKind kind = spring ? RunConfigurationKind.SPRING_BOOT : RunConfigurationKind.JAVA_APPLICATION;
        String id = (spring ? "spring:" : "java:") + mainClass;
        result.removeIf(configuration -> configuration.id().equals(id));
        result.add(new RunConfiguration(id, type.group(1), kind, root, mainClass));
    }

    private List<Path> sourceRoots(Path root) {
        List<Path> roots = new ArrayList<>();
        roots.add(root.resolve("src/main/java"));
        roots.add(root.resolve("src"));
        return roots.stream().distinct().toList();
    }

    private String packageName(String source) {
        Matcher matcher = PACKAGE.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ignored) {
            return "";
        }
    }

    private String mask(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (lineComment) {
                result.append(current == '\n' ? '\n' : ' ');
                lineComment = current != '\n';
            } else if (blockComment) {
                result.append(current == '\n' ? '\n' : ' ');
                if (current == '*' && next == '/') {
                    result.append(' ');
                    i++;
                    blockComment = false;
                }
            } else if (quote != 0) {
                result.append(current == '\n' ? '\n' : ' ');
                if (!escaped && current == quote) {
                    quote = 0;
                }
                escaped = !escaped && current == '\\';
                if (current != '\\') {
                    escaped = false;
                }
            } else if (current == '/' && next == '/') {
                result.append("  ");
                i++;
                lineComment = true;
            } else if (current == '/' && next == '*') {
                result.append("  ");
                i++;
                blockComment = true;
            } else if (current == '"' || current == '\'') {
                result.append(' ');
                quote = current;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
