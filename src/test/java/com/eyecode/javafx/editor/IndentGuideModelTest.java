package com.eyecode.javafx.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndentGuideModelTest {

    private final IndentGuideModel model = new IndentGuideModel();

    @Test
    void noIndentationProducesNoGuides() {
        IndentGuideLine line = model.lineFor("class Example {}");

        assertFalse(line.hasGuides());
        assertEquals(List.of(), line.columns());
    }

    @Test
    void oneIndentLevelFromSpacesProducesSingleGuide() {
        IndentGuideLine line = model.lineFor("    value++;");

        assertEquals(List.of(4), line.columns());
    }

    @Test
    void twoIndentLevelsFromSpacesProduceTwoGuides() {
        IndentGuideLine line = model.lineFor("        value++;");

        assertEquals(List.of(4, 8), line.columns());
    }

    @Test
    void nestedBlockLineProducesExpectedColumns() {
        IndentGuideLine line = model.lineFor("            run();");

        assertEquals(List.of(4, 8, 12), line.columns());
        assertEquals(12, line.deepestColumn());
    }

    @Test
    void tabsAdvanceByIndentSize() {
        IndentGuideLine line = model.lineFor("\t\tvalue++;");

        assertEquals(List.of(4, 8), line.columns());
    }

    @Test
    void mixedIndentationUsesLogicalColumns() {
        IndentGuideLine line = model.lineFor("  \t  value++;");

        assertEquals(List.of(4), line.columns());
    }

    @Test
    void blankLineWithOwnIndentationShowsGuides() {
        IndentGuideLine line = model.lineFor("    ");

        assertEquals(List.of(4), line.columns());
    }

    @Test
    void blankLineWithoutIndentationShowsNoGuides() {
        IndentGuideLine line = model.lineFor("");

        assertFalse(line.hasGuides());
    }

    @Test
    void positionsRemainDeterministicAcrossRepeatedQueries() {
        String source = "            value++;";

        IndentGuideLine first = model.lineFor(source);
        IndentGuideLine second = model.lineFor(source);

        assertEquals(first, second);
        assertEquals(List.of(4, 8, 12), first.columns());
    }
}
