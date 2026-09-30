package com.eyecode.language.refactor;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenamePlanTest {

    @Test
    void groupsEditsByFileAndRejectsInvalidEditRanges() {
        Path fileA = Path.of("A.java").toAbsolutePath().normalize();
        Path fileB = Path.of("B.java").toAbsolutePath().normalize();
        Map<Path, List<RenamePlan.Edit>> edits = new LinkedHashMap<>();
        edits.put(fileA, List.of(new RenamePlan.Edit(0, 4, 0, 7, "bar")));
        edits.put(fileB, List.of(new RenamePlan.Edit(1, 0, 1, 3, "bar"), new RenamePlan.Edit(2, 0, 2, 3, "baz")));
        RenamePlan plan = new RenamePlan(edits);

        assertFalse(plan.isEmpty());
        assertEquals(List.of(fileA, fileB), List.copyOf(plan.editsByFile().keySet()));
        assertEquals(2, plan.editsByFile().get(fileB).size());
        assertThrows(IllegalArgumentException.class, () -> new RenamePlan.Edit(2, 0, 1, 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> new RenamePlan.Edit(0, 5, 0, 4, "x"));
        assertThrows(IllegalArgumentException.class, () -> new RenamePlan.Edit(-1, 0, 0, 0, "x"));
    }

    @Test
    void emptyPlansAreDetected() {
        assertTrue(new RenamePlan(Map.of()).isEmpty());
        assertTrue(new RenamePlan(Map.of(Path.of("A.java"), List.of())).isEmpty());
    }

    @Test
    void prepareRenameResultValidatesRangesAndKeepsPlaceholder() {
        assertThrows(IllegalArgumentException.class, () -> new PrepareRenameResult(-1, 0, "x", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PrepareRenameResult(5, 4, "x", 0, 0));
        assertThrows(NullPointerException.class, () -> new PrepareRenameResult(0, 1, null, 0, 0));
        PrepareRenameResult result = new PrepareRenameResult(3, 6, "foo", 1, 2);
        assertEquals(3, result.startOffset());
        assertEquals(6, result.endOffset());
        assertEquals("foo", result.placeholder());
        assertEquals(1, result.line());
        assertEquals(2, result.character());
    }

    @Test
    void renameResultCarriesSuccessAppliedAndFailedFiles() {
        RenameResult success = RenameResult.success(2);
        assertTrue(success.success());
        assertEquals(2, success.applied());
        assertTrue(success.failedFiles().isEmpty());
        RenameResult failed = RenameResult.failed(0, List.of("X.java"));
        assertFalse(failed.success());
        assertEquals(List.of("X.java"), failed.failedFiles());
    }
}
