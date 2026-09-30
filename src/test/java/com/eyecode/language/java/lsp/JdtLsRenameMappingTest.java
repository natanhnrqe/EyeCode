package com.eyecode.language.java.lsp;

import com.eyecode.language.refactor.RenamePlan;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdtLsRenameMappingTest {

    @Test
    void workspaceEditChangesGroupEditsByFileInsideTheProject() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-map");
        Path fileA = Files.writeString(root.resolve("A.java"), "class A {}");
        Path fileB = Files.writeString(root.resolve("B.java"), "class B {}");
        Map<String, List<TextEdit>> changes = new LinkedHashMap<>();
        changes.put(fileA.toUri().toString(), List.of(new TextEdit(new Range(new Position(0, 6), new Position(0, 7)), "Renamed")));
        changes.put(fileB.toUri().toString(), List.of(new TextEdit(new Range(new Position(0, 6), new Position(0, 7)), "Renamed")));
        WorkspaceEdit edit = new WorkspaceEdit(changes);

        RenamePlan plan = JdtLsProjectRefactor.toPlan(edit, root).orElseThrow();

        assertEquals(List.of(fileA, fileB), List.copyOf(plan.editsByFile().keySet()));
        RenamePlan.Edit mapped = plan.editsByFile().get(fileA).get(0);
        assertEquals(0, mapped.startLine());
        assertEquals(6, mapped.startCharacter());
        assertEquals(7, mapped.endCharacter());
        assertEquals("Renamed", mapped.newText());
    }

    @Test
    void editsOutsideTheProjectAreRejected() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-root");
        Path outside = Files.createTempDirectory("eyecode-rename-outside").resolve("X.java");
        Files.writeString(outside, "class X {}");
        WorkspaceEdit edit = new WorkspaceEdit(Map.of(
                outside.toUri().toString(), List.of(new TextEdit(new Range(new Position(0, 0), new Position(0, 1)), "Y"))));

        assertTrue(JdtLsProjectRefactor.toPlan(edit, root).isEmpty());
    }

    @Test
    void malformedUrisAreSkippedAndNullOrEmptyEditsProduceNoPlan() throws Exception {
        Path root = Files.createTempDirectory("eyecode-rename-empty");
        Map<String, List<TextEdit>> changes = new LinkedHashMap<>();
        changes.put("not-a-uri", List.of(new TextEdit(new Range(new Position(0, 0), new Position(0, 1)), "Y")));
        WorkspaceEdit onlyMalformed = new WorkspaceEdit(changes);

        assertTrue(JdtLsProjectRefactor.toPlan(onlyMalformed, root).isEmpty());
        assertTrue(JdtLsProjectRefactor.toPlan(new WorkspaceEdit(), root).isEmpty());
        assertTrue(JdtLsProjectRefactor.toPlan(null, root).isEmpty());
    }
}
