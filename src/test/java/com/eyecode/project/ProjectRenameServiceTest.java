package com.eyecode.project;

import com.eyecode.language.refactor.RenamePlan;
import com.eyecode.language.refactor.RenameResult;
import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRenameServiceTest {

    private final ProjectRenameService service = new ProjectRenameService(new ProjectFileOperationService());

    @Test
    void validatesJavaIdentifiers() {
        assertTrue(service.isValidIdentifier("foo"));
        assertTrue(service.isValidIdentifier("_bar9"));
        assertFalse(service.isValidIdentifier("9lives"));
        assertFalse(service.isValidIdentifier("has space"));
        assertFalse(service.isValidIdentifier(null));
        assertThrows(IllegalArgumentException.class, () -> service.requireValidIdentifier("has space"));
    }

    @Test
    void renameAcrossFilesUpdatesEverySemanticOccurrence() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-multi");
        String sourceA = "class A { int counter = 0; void run() { counter++; } }\n";
        String sourceB = "class B { int counter = 1; }\n";
        Path fileA = Files.writeString(root.resolve("A.java"), sourceA);
        Path fileB = Files.writeString(root.resolve("B.java"), sourceB);
        int first = sourceA.indexOf("counter");
        int second = sourceA.indexOf("counter", first + 1);
        RenamePlan plan = new RenamePlan(Map.of(
                fileA, List.of(
                        editAt(sourceA, first, "counter", "total"),
                        editAt(sourceA, second, "counter", "total")),
                fileB, List.of(editAt(sourceB, sourceB.indexOf("counter"), "counter", "total"))));

        RenameResult result = service.rename(ProjectModel.fromDirectory(root.toFile()), plan);

        assertTrue(result.success());
        assertEquals(2, result.applied());
        assertTrue(result.failedFiles().isEmpty());
        assertTrue(Files.readString(fileA).contains("total++"), Files.readString(fileA));
        assertEquals(2, countOccurrences(Files.readString(fileA), "total"));
        assertTrue(Files.readString(fileB).contains("int total = 1;"));
    }

    @Test
    void renameToTheSameNameIsRejectedAsNoChange() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-same");
        String source = "class A { int foo = 0; }\n";
        Path fileA = Files.writeString(root.resolve("A.java"), source);
        RenamePlan plan = new RenamePlan(Map.of(fileA, List.of(editAt(source, source.indexOf("foo"), "foo", "foo"))));

        RenameResult result = service.rename(ProjectModel.fromDirectory(root.toFile()), plan);

        assertFalse(result.success());
        assertEquals(0, result.applied());
        assertEquals(source, Files.readString(fileA));
    }

    @Test
    void partialFailureRollsBackPreviouslyAppliedFiles() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-rollback");
        String sourceA = "class A { int foo = 0; }\n";
        String sourceB = "class B { int foo = 1; }\n";
        Path fileA = Files.writeString(root.resolve("A.java"), sourceA);
        Path fileB = Files.writeString(root.resolve("B.java"), sourceB);
        Map<Path, List<RenamePlan.Edit>> edits = new LinkedHashMap<>();
        edits.put(fileA, List.of(editAt(sourceA, sourceA.indexOf("foo"), "foo", "bar")));
        edits.put(fileB, List.of(editAt(sourceB, sourceB.indexOf("foo"), "foo", "bar")));
        fileB.toFile().setReadOnly();
        try {
            RenameResult result = service.rename(ProjectModel.fromDirectory(root.toFile()), new RenamePlan(edits));

            assertFalse(result.success());
            assertEquals(0, result.applied());
            assertFalse(result.failedFiles().isEmpty());
            assertTrue(result.failedFiles().stream().anyMatch(failed -> failed.endsWith("B.java")));
            assertEquals(sourceA, Files.readString(fileA));
            assertEquals(sourceB, Files.readString(fileB));
        } finally {
            fileB.toFile().setWritable(true);
        }
    }

    @Test
    void emptyPlanSucceedsWithoutTouchingFiles() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-empty");
        Path fileA = Files.writeString(root.resolve("A.java"), "class A {}\n");

        RenameResult result = service.rename(ProjectModel.fromDirectory(root.toFile()), new RenamePlan(Map.of()));

        assertTrue(result.success());
        assertEquals(0, result.applied());
        assertEquals("class A {}\n", Files.readString(fileA));
    }

    private static RenamePlan.Edit editAt(String source, int offset, String oldText, String newText) {
        int line = 0;
        int lineStart = 0;
        for (int index = 0; index < offset; index++) {
            if (source.charAt(index) == '\n') {
                line++;
                lineStart = index + 1;
            }
        }
        int column = offset - lineStart;
        return new RenamePlan.Edit(line, column, line, column + oldText.length(), newText);
    }

    private static int countOccurrences(String text, String word) {
        int count = 0;
        for (int index = text.indexOf(word); index >= 0; index = text.indexOf(word, index + word.length())) count++;
        return count;
    }
}
