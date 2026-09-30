package com.eyecode.project;

import com.eyecode.language.refactor.RenamePlan;
import com.eyecode.language.refactor.RenameResult;
import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ProjectRenameService {
    public static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private final ProjectFileOperationService fileOperations;

    public ProjectRenameService(ProjectFileOperationService fileOperations) {
        this.fileOperations = Objects.requireNonNull(fileOperations, "fileOperations");
    }

    public boolean isValidIdentifier(String name) {
        return name != null && JAVA_IDENTIFIER.matcher(name).matches();
    }

    public void requireValidIdentifier(String name) {
        if (!isValidIdentifier(name)) {
            throw new IllegalArgumentException("Novo nome invalido: deve ser um identificador Java valido");
        }
    }

    public RenameResult rename(ProjectModel project, RenamePlan plan) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(plan, "plan");
        if (plan.isEmpty()) return RenameResult.success(0);
        List<Path> files = new ArrayList<>(plan.editsByFile().keySet());
        List<String> failed = new ArrayList<>();
        Map<Path, String> originals = new LinkedHashMap<>();
        for (int index = 0; index < files.size(); index++) {
            Path file = files.get(index);
            try {
                String original = fileOperations.applyTextEdits(project, file, plan.editsByFile().get(file));
                originals.put(file, original);
            } catch (IOException | RuntimeException exception) {
                failed.add(file.toString());
                for (int remaining = index + 1; remaining < files.size(); remaining++) {
                    failed.add(files.get(remaining).toString());
                }
                rollback(originals);
                return RenameResult.failed(0, failed);
            }
        }
        int changed = 0;
        for (Map.Entry<Path, String> entry : originals.entrySet()) {
            try {
                if (!Files.readString(entry.getKey(), StandardCharsets.UTF_8).equals(entry.getValue())) changed++;
            } catch (IOException ignored) {
                changed++;
            }
        }
        if (changed == 0) return RenameResult.failed(0, List.of());
        return RenameResult.success(changed);
    }

    private void rollback(Map<Path, String> originals) {
        originals.forEach((file, original) -> {
            try {
                Files.writeString(file, original, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        });
    }
}
