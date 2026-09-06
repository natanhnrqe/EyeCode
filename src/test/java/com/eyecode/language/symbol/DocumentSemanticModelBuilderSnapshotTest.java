package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSemanticModelBuilderSnapshotTest {

    @Test
    void buildsSemanticFactsFromTheSuppliedSnapshotAfterAnUnsavedEdit() {
        EditorDocument document = new EditorDocument(Path.of("Live.java"), "class OldType {}");
        DocumentSnapshot beforeEdit = document.snapshot();
        document.setText("class NewType {}");
        DocumentSnapshot afterEdit = document.snapshot();
        DocumentSemanticModelBuilder builder = new DocumentSemanticModelBuilder();

        SymbolTable before = builder.build(beforeEdit).orElseThrow().symbolTable();
        SymbolTable after = builder.build(afterEdit).orElseThrow().symbolTable();

        assertTrue(before.lookup(before.rootScope().id(), "OldType").isPresent());
        assertFalse(before.lookup(before.rootScope().id(), "NewType").isPresent());
        assertTrue(after.lookup(after.rootScope().id(), "NewType").isPresent());
        assertFalse(after.lookup(after.rootScope().id(), "OldType").isPresent());
    }
}
