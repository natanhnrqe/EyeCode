package com.eyecode.language.navigation;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.java.lsp.JdtLsDocumentSync;
import com.eyecode.language.java.lsp.JdtLsLifecycleState;
import com.eyecode.language.java.lsp.JdtLsSession;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdtLsNavigationService implements NavigationService {
    private static final Duration NAVIGATION_TIMEOUT = Duration.ofSeconds(5);

    private final JdtLsSession session;
    private final JdtLsDocumentSync documents;

    public JdtLsNavigationService(JdtLsSession session, JdtLsDocumentSync documents) {
        this.session = Objects.requireNonNull(session, "session must not be null");
        this.documents = Objects.requireNonNull(documents, "documents must not be null");
    }

    @Override
    public synchronized List<NavigationTarget> definition(Path file, String source, long version, int line, int column) {
        if (file == null || session.state() != JdtLsLifecycleState.READY || !session.supportsDefinition()) {
            return List.of();
        }
        try {
            String requestUri = documents.synchronize(session, file, source, version);
            return toTargets(session.definition(requestUri, line, column, NAVIGATION_TIMEOUT), requestUri, source);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @Override
    public synchronized List<NavigationTarget> references(Path file, String source, long version, int line,
                                                          int column, boolean includeDeclaration) {
        if (file == null || session.state() != JdtLsLifecycleState.READY || !session.supportsReferences()) {
            return List.of();
        }
        try {
            String requestUri = documents.synchronize(session, file, source, version);
            return toTargetList(session.references(requestUri, line, column, includeDeclaration, NAVIGATION_TIMEOUT),
                    requestUri, source);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    static List<NavigationTarget> toTargets(Either<List<? extends Location>, List<? extends LocationLink>> result,
                                            String requestUri, String requestSource) {
        if (result == null) return List.of();
        if (result.isLeft()) return toTargetList(result.getLeft(), requestUri, requestSource);
        if (result.isRight()) return toLinkTargets(result.getRight(), requestUri, requestSource);
        return List.of();
    }

    static List<NavigationTarget> toTargetList(List<? extends Location> locations, String requestUri,
                                                String requestSource) {
        if (locations == null) return List.of();
        List<NavigationTarget> targets = new ArrayList<>();
        for (Location location : locations) {
            if (location == null) continue;
            addTarget(targets, location.getUri(), location.getRange(), location.getRange(), requestUri, requestSource);
        }
        return List.copyOf(targets);
    }

    static List<NavigationTarget> toLinkTargets(List<? extends LocationLink> links, String requestUri,
                                                 String requestSource) {
        if (links == null) return List.of();
        List<NavigationTarget> targets = new ArrayList<>();
        for (LocationLink link : links) {
            if (link == null) continue;
            Range selection = link.getTargetSelectionRange() == null
                    ? link.getTargetRange() : link.getTargetSelectionRange();
            addTarget(targets, link.getTargetUri(), link.getTargetRange(), selection, requestUri, requestSource);
        }
        return List.copyOf(targets);
    }

    private static void addTarget(List<NavigationTarget> targets, String uri, Range range, Range selection,
                                  String requestUri, String requestSource) {
        if (uri == null || uri.isBlank() || range == null || selection == null) return;
        String content = contentOf(uri, requestUri, requestSource);
        if (content == null) return;
        TextRange targetRange = offsets(range, content);
        TextRange targetSelection = offsets(selection, content);
        if (targetRange == null || targetSelection == null) return;
        targets.add(new NavigationTarget(uri, targetRange, targetSelection));
    }

    public static String contentOf(String uri, String requestUri, String requestSource) {
        if (uri == null || uri.isBlank()) return null;
        if (uri.equals(requestUri) && requestSource != null) return requestSource;
        try {
            URI parsed = URI.create(uri);
            if (!"file".equalsIgnoreCase(parsed.getScheme())) return null;
            return Files.readString(Path.of(parsed));
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    static TextRange offsets(Range range, String content) {
        if (range.getStart() == null || range.getEnd() == null) return null;
        LineMap lines = LineMap.of(content);
        int start = clampedOffset(lines, range.getStart().getLine(), range.getStart().getCharacter());
        int end = clampedOffset(lines, range.getEnd().getLine(), range.getEnd().getCharacter());
        if (start < 0 || end < 0 || start > end) return null;
        return TextRange.of(start, end);
    }

    private static int clampedOffset(LineMap lines, int line, int character) {
        if (line < 0 || character < 0) return -1;
        return lines.offsetOf(line, character);
    }
}
