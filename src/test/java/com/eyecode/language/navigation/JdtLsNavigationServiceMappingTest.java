package com.eyecode.language.navigation;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsNavigationServiceMappingTest {
    private static final String REQUEST_URI = "file:///req/Main.java";
    private static final String REQUEST_SOURCE = "class Main {\n    void run() {\n        int value = 1;\n    }\n}";

    @TempDir
    Path temporary;

    @Test
    void plainLocationsMapToOffsetRangesAgainstTheRequestSource() {
        Location location = new Location(REQUEST_URI, new Range(new Position(2, 13), new Position(2, 18)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargetList(List.of(location), REQUEST_URI, REQUEST_SOURCE);

        LineMap lines = LineMap.of(REQUEST_SOURCE);
        assertEquals(1, targets.size());
        assertEquals(REQUEST_URI, targets.getFirst().uri());
        assertEquals(lines.offsetOf(2, 13), targets.getFirst().range().startOffset());
        assertEquals(lines.offsetOf(2, 18), targets.getFirst().range().endOffset());
        assertEquals(targets.getFirst().range(), targets.getFirst().selectionRange());
    }

    @Test
    void eitherLeftBranchMapsPlainLocations() {
        Location location = new Location(REQUEST_URI, new Range(new Position(0, 0), new Position(0, 5)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargets(
                Either.forLeft(List.of(location)), REQUEST_URI, REQUEST_SOURCE);

        assertEquals(1, targets.size());
        assertEquals(REQUEST_URI, targets.getFirst().uri());
    }

    @Test
    void eitherRightBranchMapsLocationLinks() {
        LocationLink link = new LocationLink();
        link.setTargetUri(REQUEST_URI);
        link.setTargetRange(new Range(new Position(2, 8), new Position(3, 5)));
        link.setTargetSelectionRange(new Range(new Position(2, 13), new Position(2, 18)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargets(
                Either.forRight(List.of(link)), REQUEST_URI, REQUEST_SOURCE);

        LineMap lines = LineMap.of(REQUEST_SOURCE);
        assertEquals(1, targets.size());
        assertEquals(lines.offsetOf(2, 8), targets.getFirst().range().startOffset());
        assertEquals(lines.offsetOf(3, 5), targets.getFirst().range().endOffset());
        assertEquals(lines.offsetOf(2, 13), targets.getFirst().selectionRange().startOffset());
        assertEquals(lines.offsetOf(2, 18), targets.getFirst().selectionRange().endOffset());
    }

    @Test
    void linkWithoutSelectionRangeFallsBackToTheTargetRange() {
        LocationLink link = new LocationLink();
        link.setTargetUri(REQUEST_URI);
        link.setTargetRange(new Range(new Position(0, 0), new Position(0, 5)));

        List<NavigationTarget> targets = JdtLsNavigationService.toLinkTargets(List.of(link), REQUEST_URI, REQUEST_SOURCE);

        assertEquals(1, targets.size());
        assertEquals(targets.getFirst().range(), targets.getFirst().selectionRange());
    }

    @Test
    void invalidTargetsAreFiltered() {
        List<Location> locations = new ArrayList<>();
        Location missingUri = new Location();
        missingUri.setRange(new Range(new Position(0, 0), new Position(0, 1)));
        locations.add(missingUri);
        locations.add(new Location("  ", new Range(new Position(0, 0), new Position(0, 1))));
        Location missingRange = new Location();
        missingRange.setUri(REQUEST_URI);
        locations.add(missingRange);
        locations.add(null);
        Range negative = new Range(new Position(0, -1), new Position(0, 1));
        locations.add(new Location(REQUEST_URI, negative));
        Location nullStart = new Location(REQUEST_URI, new Range());
        locations.add(nullStart);

        assertTrue(JdtLsNavigationService.toTargetList(locations, REQUEST_URI, REQUEST_SOURCE).isEmpty());
    }

    @Test
    void nonFileSchemeTargetsAreFiltered() {
        Location jdk = new Location("jdt://contents/java.lang/String.class?", new Range(new Position(0, 0), new Position(0, 1)));

        assertTrue(JdtLsNavigationService.toTargetList(List.of(jdk), REQUEST_URI, REQUEST_SOURCE).isEmpty());
    }

    @Test
    void unreadableFileTargetsAreFiltered() {
        Location missing = new Location(temporary.resolve("Ausente.java").toUri().toString(),
                new Range(new Position(0, 0), new Position(0, 1)));

        assertTrue(JdtLsNavigationService.toTargetList(List.of(missing), REQUEST_URI, REQUEST_SOURCE).isEmpty());
    }

    @Test
    void crossFileTargetsAreMappedAgainstTheTargetFileContent() throws Exception {
        Path other = Files.writeString(temporary.resolve("Servico.java"), "public class Servico {\n    void saudacao() {\n    }\n}");
        String otherUri = other.toUri().toString();
        Location location = new Location(otherUri, new Range(new Position(1, 9), new Position(1, 17)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargetList(List.of(location), REQUEST_URI, REQUEST_SOURCE);

        String content = Files.readString(other);
        LineMap lines = LineMap.of(content);
        assertEquals(1, targets.size());
        assertEquals(otherUri, targets.getFirst().uri());
        assertEquals("saudacao", content.substring(targets.getFirst().range().startOffset(), targets.getFirst().range().endOffset()));
        assertEquals(lines.offsetOf(1, 9), targets.getFirst().range().startOffset());
        assertEquals(lines.offsetOf(1, 17), targets.getFirst().range().endOffset());
    }

    @Test
    void rangesBeyondTheEndOfContentAreClamped() {
        Location location = new Location(REQUEST_URI, new Range(new Position(40, 80), new Position(60, 90)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargetList(List.of(location), REQUEST_URI, REQUEST_SOURCE);

        assertEquals(1, targets.size());
        assertEquals(REQUEST_SOURCE.length(), targets.getFirst().range().startOffset());
        assertEquals(REQUEST_SOURCE.length(), targets.getFirst().range().endOffset());
    }

    @Test
    void mixedValidityKeepsOnlyTheValidTargetsInOrder() {
        Location valid = new Location(REQUEST_URI, new Range(new Position(0, 0), new Position(0, 5)));
        Location invalid = new Location("jdt://x", new Range(new Position(0, 0), new Position(0, 1)));
        Location validTwo = new Location(REQUEST_URI, new Range(new Position(1, 4), new Position(1, 8)));

        List<NavigationTarget> targets = JdtLsNavigationService.toTargetList(
                List.of(valid, invalid, validTwo), REQUEST_URI, REQUEST_SOURCE);

        assertEquals(2, targets.size());
        assertEquals(0, targets.getFirst().range().startOffset());
        assertEquals(LineMap.of(REQUEST_SOURCE).offsetOf(1, 4), targets.get(1).range().startOffset());
    }

    @Test
    void contentOfPrefersTheRequestSourceForTheRequestUri() {
        assertEquals(REQUEST_SOURCE, JdtLsNavigationService.contentOf(REQUEST_URI, REQUEST_URI, REQUEST_SOURCE));
        assertNull(JdtLsNavigationService.contentOf(REQUEST_URI, REQUEST_URI, null));
        assertNull(JdtLsNavigationService.contentOf(null, REQUEST_URI, REQUEST_SOURCE));
        assertNull(JdtLsNavigationService.contentOf("jdt://contents/X", REQUEST_URI, REQUEST_SOURCE));
    }

    @Test
    void offsetsRejectNegativePositionsAndInvertedRanges() {
        assertNull(JdtLsNavigationService.offsets(new Range(new Position(-1, 0), new Position(0, 1)), REQUEST_SOURCE));
        assertNull(JdtLsNavigationService.offsets(new Range(new Position(0, 0), new Position(-3, 1)), REQUEST_SOURCE));

        TextRange clamped = JdtLsNavigationService.offsets(
                new Range(new Position(0, 0), new Position(9, 99)), REQUEST_SOURCE);
        assertEquals(REQUEST_SOURCE.length(), clamped.endOffset());
    }

    @Test
    void nullEitherResultProducesNoTargets() {
        assertTrue(JdtLsNavigationService.toTargets(null, REQUEST_URI, REQUEST_SOURCE).isEmpty());
        assertTrue(JdtLsNavigationService.toTargetList(null, REQUEST_URI, REQUEST_SOURCE).isEmpty());
        assertTrue(JdtLsNavigationService.toLinkTargets(null, REQUEST_URI, REQUEST_SOURCE).isEmpty());
    }
}
