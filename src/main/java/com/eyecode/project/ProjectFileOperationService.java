package com.eyecode.project;

import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProjectFileOperationService {

    public record RenameResult(Path oldPath, Path newPath, String source) {}

    public Path createFile(ProjectModel project, Path directory, String requestedName) throws IOException {
        Path parent = requireDirectory(project, directory);
        Path target = parent.resolve(validateName(requestedName)).normalize();
        requireInside(project, target);
        if (Files.exists(target)) throw new IllegalArgumentException("A sibling with that name already exists");
        return Files.createFile(target);
    }

    public Path createDirectory(ProjectModel project, Path directory, String requestedName) throws IOException {
        Path parent = requireDirectory(project, directory);
        Path target = parent.resolve(validateName(requestedName)).normalize();
        requireInside(project, target);
        if (Files.exists(target)) throw new IllegalArgumentException("A sibling with that name already exists");
        return Files.createDirectory(target);
    }

    public Path createPackage(ProjectModel project, Path directory, String packageName) throws IOException {
        Path parent = requireDirectory(project, directory);
        Path sourceRoot = root(project).resolve("src/main/java");
        if (!parent.startsWith(sourceRoot)) throw new IllegalArgumentException("Packages must be created under src/main/java");
        String value = packageName == null ? "" : packageName.trim();
        if (value.isBlank()) throw new IllegalArgumentException("Package name is required");
        Path target = parent;
        for (String segment : value.split("\\.")) {
            if (!isJavaIdentifier(segment)) throw new IllegalArgumentException("Invalid Java package name");
            target = target.resolve(segment);
        }
        requireInside(project, target);
        return Files.createDirectories(target);
    }

    public Path createJavaClass(ProjectModel project, Path directory, String requestedName) throws IOException {
        Path parent = requireDirectory(project, directory);
        String typeName = requestedName == null ? "" : requestedName.trim();
        if (typeName.endsWith(".java")) typeName = typeName.substring(0, typeName.length() - 5);
        if (!isJavaIdentifier(typeName)) throw new IllegalArgumentException("Invalid Java type name");
        Path target = parent.resolve(typeName + ".java").normalize();
        requireInside(project, target);
        if (Files.exists(target)) throw new IllegalArgumentException("A sibling with that name already exists");
        String packageName = packageName(project, parent);
        String source = packageName.isBlank() ? "public class " + typeName + " {\n\n}\n"
                : "package " + packageName + ";\n\npublic class " + typeName + " {\n\n}\n";
        return Files.writeString(target, source, StandardCharsets.UTF_8);
    }

    public Path duplicate(ProjectModel project, Path source) throws IOException {
        Path safe = requireTarget(project, source);
        if (!Files.isRegularFile(safe)) throw new IllegalArgumentException("Only files can be duplicated");
        String fileName = safe.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String base = extension > 0 ? fileName.substring(0, extension) : fileName;
        String suffix = extension > 0 ? fileName.substring(extension) : "";
        Path target = safe.resolveSibling(base + " copy" + suffix);
        for (int index = 2; Files.exists(target); index++) {
            target = safe.resolveSibling(base + " copy " + index + suffix);
        }
        return Files.copy(safe, target);
    }

    public void delete(ProjectModel project, Path target) throws IOException {
        Path safe = requireTarget(project, target);
        if (safe.equals(root(project))) {
            throw new IllegalArgumentException("The project root cannot be deleted");
        }
        if (Files.isSymbolicLink(safe)) {
            Files.deleteIfExists(safe);
            return;
        }
        if (Files.isDirectory(safe)) {
            try (var stream = Files.walk(safe)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new OperationException(exception);
                    }
                });
            } catch (OperationException exception) {
                throw exception.cause;
            }
        } else {
            Files.deleteIfExists(safe);
        }
    }

    public RenameResult rename(ProjectModel project, Path source, String newName) throws IOException {
        Path oldPath = requireTarget(project, source);
        if (oldPath.equals(root(project))) {
            throw new IllegalArgumentException("The project root cannot be renamed");
        }
        String name = validateName(newName);
        if (Files.isDirectory(oldPath) && name.contains(".")) {
            throw new IllegalArgumentException("Invalid directory name");
        }
        Path destination = oldPath.resolveSibling(name).normalize();
        requireInside(project, destination);
        if (Files.exists(destination)) {
            throw new IllegalArgumentException("A sibling with that name already exists");
        }
        String sourceText = null;
        if (Files.isRegularFile(oldPath) && oldPath.getFileName().toString().endsWith(".java")) {
            sourceText = Files.readString(oldPath, StandardCharsets.UTF_8);
            String oldType = stripJavaExtension(oldPath.getFileName().toString());
            String newType = stripJavaExtension(name);
            if (!isJavaIdentifier(newType)) {
                throw new IllegalArgumentException("Invalid Java type name");
            }
            String updated = renameJavaType(sourceText, oldType, newType);
            if (!updated.equals(sourceText)) {
                Files.writeString(oldPath, updated, StandardCharsets.UTF_8);
                sourceText = updated;
            }
        }
        try {
            Files.move(oldPath, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(oldPath, destination);
        }
        return new RenameResult(oldPath, destination, sourceText);
    }

    public Path requireTarget(ProjectModel project, Path target) {
        if (project == null || target == null) throw new IllegalArgumentException("Project and target are required");
        Path safe = target.toAbsolutePath().normalize();
        requireInside(project, safe);
        if (!Files.exists(safe)) throw new IllegalArgumentException("Target does not exist");
        return safe;
    }

    private Path requireDirectory(ProjectModel project, Path target) {
        Path safe = requireTarget(project, target);
        if (!Files.isDirectory(safe)) throw new IllegalArgumentException("Target must be a directory");
        return safe;
    }

    private Path root(ProjectModel project) {
        return project.getRootDir().toAbsolutePath().normalize();
    }

    private void requireInside(ProjectModel project, Path target) {
        Path projectRoot = root(project);
        if (!target.startsWith(projectRoot)) throw new IllegalArgumentException("Target is outside the project");
        try {
            Path realRoot = projectRoot.toRealPath();
            Path existing = Files.exists(target) ? target.toRealPath() : target.getParent().toRealPath().resolve(target.getFileName());
            if (!existing.normalize().startsWith(realRoot)) throw new IllegalArgumentException("Target escapes the project");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot validate target path", exception);
        }
    }

    private String validateName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")
                || hasForbiddenNameCharacter(name) || Path.of(name).isAbsolute()) {
            throw new IllegalArgumentException("Invalid name");
        }
        return name;
    }

    private String renameJavaType(String source, String oldName, String newName) {
        List<Token> tokens = new JavaLexer().tokenize(source);
        List<Token> code = tokens.stream().filter(token -> token.type() != JavaTokenType.WHITESPACE
                && token.type() != JavaTokenType.COMMENT).toList();
        int depth = 0;
        int declarationIndex = -1;
        for (int i = 0; i + 1 < code.size(); i++) {
            Token token = code.get(i);
            if (token.text().equals("{")) depth++;
            if (token.text().equals("}")) depth--;
            if (depth == 0 && (token.text().equals("class") || token.text().equals("interface")
                    || token.text().equals("enum") || token.text().equals("record"))
                    && code.get(i + 1).text().equals(oldName)) {
                declarationIndex = i + 1;
                break;
            }
        }
        if (declarationIndex < 0) return source;
        List<int[]> replacements = new ArrayList<>();
        replacements.add(new int[]{code.get(declarationIndex).startOffset(), code.get(declarationIndex).endOffset()});
        int bodyDepth = 0;
        boolean bodyStarted = false;
        for (int i = declarationIndex + 1; i < code.size(); i++) {
            String text = code.get(i).text();
            if (text.equals("{")) {
                bodyStarted = true;
                bodyDepth++;
            } else if (text.equals("}")) {
                bodyDepth--;
            } else if (bodyStarted && bodyDepth == 1 && text.equals(oldName)
                    && i + 1 < code.size() && code.get(i + 1).text().equals("(")) {
                replacements.add(new int[]{code.get(i).startOffset(), code.get(i).endOffset()});
            }
            if (bodyStarted && bodyDepth == 0) break;
        }
        StringBuilder result = new StringBuilder(source);
        replacements.sort(Comparator.comparingInt(value -> -value[0]));
        for (int[] replacement : replacements) result.replace(replacement[0], replacement[1], newName);
        return result.toString();
    }

    private String stripJavaExtension(String name) {
        return name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
    }

    private boolean isJavaIdentifier(String value) {
        return value != null && SourceVersion.isIdentifier(value) && !SourceVersion.isKeyword(value);
    }

    private boolean hasForbiddenNameCharacter(String name) {
        for (int index = 0; index < name.length(); index++) {
            char value = name.charAt(index);
            if (value < 32 || "<>:\"/\\|?*".indexOf(value) >= 0) return true;
        }
        return false;
    }

    private String packageName(ProjectModel project, Path directory) {
        Path root = root(project);
        Path sourceRoot = root.resolve("src/main/java");
        if (!directory.startsWith(sourceRoot)) return "";
        Path relative = sourceRoot.relativize(directory);
        return relative.toString().replace('\\', '.').replace('/', '.');
    }

    private static final class OperationException extends RuntimeException {
        private final IOException cause;
        private OperationException(IOException cause) { this.cause = cause; }
    }
}
