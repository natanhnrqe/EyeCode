package com.eyecode.project;

import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProjectCreationService {

    public enum JavaTypeKind { CLASS, INTERFACE, ENUM, RECORD, MAIN_CLASS }

    public record CreationContext(ProjectModel project, Path selectedDirectory) {
        public CreationContext {
            if (project == null || selectedDirectory == null) {
                throw new IllegalArgumentException("Project and selected directory are required");
            }
        }
    }

    public record CreationResult(Path path, String source, int caretOffset) {}

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
            "var", "yield", "record", "sealed", "permits", "non-sealed"
    );

    public CreationResult createPackage(CreationContext context, String packageInput) throws IOException {
        String packageName = packageNameFor(context, packageInput);
        Path sourceRoot = sourceRootFor(context.project(), context.selectedDirectory());
        Path packageDirectory = sourceRoot.resolve(packageName.replace('.', sourceRoot.getFileSystem().getSeparator().charAt(0))).normalize();
        if (!packageDirectory.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("Package escapes the Java source root");
        }
        Files.createDirectories(packageDirectory);
        return new CreationResult(packageDirectory, "", 0);
    }

    public CreationResult createJavaType(CreationContext context, JavaTypeKind kind, String name) throws IOException {
        if (kind == null) {
            throw new IllegalArgumentException("Java type kind is required");
        }
        String typeName = normalizeTypeName(name);
        Path directory = requireJavaDirectory(context);
        String packageName = packageNameFor(context, "");
        String source = template(kind, typeName, packageName);
        Path target = uniqueTarget(directory, typeName + ".java");
        Files.writeString(target, source, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        int declarationEnd = kind == JavaTypeKind.MAIN_CLASS
                ? source.indexOf("main(String[] args) {") + "main(String[] args) {".length()
                : source.indexOf("public ", source.indexOf("\n\n") + 2);
        int caret = source.indexOf("\n", declarationEnd) + 1;
        return new CreationResult(target, source, caret);
    }

    public CreationResult createJavaFile(CreationContext context, String name) throws IOException {
        String fileName = normalizeFileName(name);
        Path directory = context.selectedDirectory().toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Selected location is not a directory");
        }
        String packageName = isJavaSourceDirectory(context.project(), directory)
                ? packageNameFor(context, "") : "";
        String source = packageDeclaration(packageName) + (packageName.isBlank() ? "" : "\n") + "\n";
        Path target = uniqueTarget(directory, fileName);
        Files.writeString(target, source, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return new CreationResult(target, source, source.length());
    }

    public String packageNameFor(CreationContext context, String packageInput) {
        Path sourceRoot = sourceRootFor(context.project(), context.selectedDirectory());
        Path selected = context.selectedDirectory().toAbsolutePath().normalize();
        String current = packageFromPath(sourceRoot, selected);
        String input = packageInput == null ? "" : packageInput.trim();
        if (!input.isBlank()) {
            validatePackage(input);
            current = input.contains(".") || current.isBlank() ? input : current + "." + input;
        }
        if (!current.isBlank()) {
            validatePackage(current);
        }
        return current;
    }

    public Path sourceRootFor(ProjectModel project, Path selectedDirectory) {
        Path selected = selectedDirectory.toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        project.getModules().stream().flatMap(module -> module.getSourceRoots().stream()).forEach(candidates::add);
        Path root = project.getRootDir().toAbsolutePath().normalize();
        candidates.add(root.resolve("src/main/java"));
        candidates.add(root.resolve("src/test/java"));
        Path conventionalSrc = root.resolve("src");
        if (Files.isDirectory(conventionalSrc) && isStandardSrcLocation(selected, conventionalSrc)) {
            candidates.add(conventionalSrc);
        }
        return candidates.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .filter(selected::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount))
                .orElseThrow(() -> new IllegalArgumentException("No Java source root for selected location"));
    }

    private boolean isStandardSrcLocation(Path selected, Path src) {
        if (!selected.startsWith(src)) {
            return false;
        }
        Path relative = src.relativize(selected);
        if (relative.getNameCount() < 2) {
            return true;
        }
        String first = relative.getName(0).toString();
        String second = relative.getName(1).toString();
        return !("main".equals(first) || "test".equals(first)) || !"resources".equals(second);
    }

    public boolean isJavaSourceDirectory(ProjectModel project, Path selectedDirectory) {
        try {
            sourceRootFor(project, selectedDirectory);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Path requireJavaDirectory(CreationContext context) {
        Path directory = context.selectedDirectory().toAbsolutePath().normalize();
        if (!isJavaSourceDirectory(context.project(), directory)) {
            throw new IllegalArgumentException("Java types must be created below a Java source root");
        }
        return directory;
    }

    private String packageFromPath(Path sourceRoot, Path selected) {
        Path relative = sourceRoot.relativize(selected);
        if (relative.getNameCount() == 0) {
            return "";
        }
        StringBuilder packageName = new StringBuilder();
        for (Path component : relative) {
            if (packageName.length() > 0) {
                packageName.append('.');
            }
            packageName.append(component.toString());
        }
        String value = packageName.toString();
        validatePackage(value);
        return value;
    }

    private void validatePackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        for (String component : packageName.split("\\.", -1)) {
            if (!isIdentifier(component)) {
                throw new IllegalArgumentException("Invalid Java package: " + packageName);
            }
        }
    }

    private String normalizeTypeName(String name) {
        String value = name == null ? "" : name.trim();
        if (!isIdentifier(value)) {
            throw new IllegalArgumentException("Invalid Java type name");
        }
        return value;
    }

    private String normalizeFileName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.endsWith(".java")) {
            value = value.substring(0, value.length() - 5);
        }
        if (!isIdentifier(value)) {
            throw new IllegalArgumentException("Invalid Java file name");
        }
        return value + ".java";
    }

    private boolean isIdentifier(String value) {
        if (value == null || value.isBlank() || JAVA_KEYWORDS.contains(value)
                || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Path uniqueTarget(Path directory, String fileName) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName).normalize();
        if (Files.exists(target)) {
            throw new IOException(fileName + " already exists.");
        }
        return target;
    }

    private String template(JavaTypeKind kind, String name, String packageName) {
        String declaration = switch (kind) {
            case CLASS, MAIN_CLASS -> "public class " + name;
            case INTERFACE -> "public interface " + name;
            case ENUM -> "public enum " + name;
            case RECORD -> "public record " + name + "()";
        };
        String body = kind == JavaTypeKind.MAIN_CLASS
                ? "    public static void main(String[] args) {\n\n    }\n"
                : "";
        return packageDeclaration(packageName) + (packageName.isBlank() ? "" : "\n")
                + "\n" + declaration + " {\n" + body + "}\n";
    }

    private String packageDeclaration(String packageName) {
        return packageName == null || packageName.isBlank() ? "" : "package " + packageName + ";";
    }
}
