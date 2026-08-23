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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProjectFileOperationService {

    public record RenameResult(Path oldPath, Path newPath, String source) {}

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
                || name.contains("/") || name.contains("\\") || Path.of(name).isAbsolute()) {
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
        if (value.isBlank() || !Character.isJavaIdentifierStart(value.charAt(0))) return false;
        for (int i = 1; i < value.length(); i++) if (!Character.isJavaIdentifierPart(value.charAt(i))) return false;
        return true;
    }

    private static final class OperationException extends RuntimeException {
        private final IOException cause;
        private OperationException(IOException cause) { this.cause = cause; }
    }
}
