package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Smart END navigation (IntelliJ-style): an END key press respects the logical
 * end of the line instead of always jumping past trailing whitespace.
 * <ul>
 *   <li>caret before the last non-whitespace character → just past it (the
 *       logical line end, trailing whitespace left untouched);</li>
 *   <li>caret already at the logical end and the line has trailing whitespace →
 *       the absolute line end (second END toggles);</li>
 *   <li>blank / whitespace-only lines and plain lines → the absolute line end.</li>
 * </ul>
 * Trailing whitespace is never removed and the document text is never changed:
 * the strategy only produces a caret movement (no undo entry).
 */
public final class SmartEndStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainKeyPressed(event, "END", true);
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        String text = snapshot.getText();
        LineMap map = snapshot.lineMap();
        int offset = Math.min(event.offset(), text.length());
        int line = map.lineOfOffset(offset);
        int lineStart = map.lineStartOffset(line);
        int lineEnd = map.lineEndOffset(line);
        int contentEnd = lastNonWhitespaceEnd(text, lineStart, lineEnd);

        int target;
        if (contentEnd == lineStart) {
            target = lineEnd;
        } else if (offset < contentEnd) {
            target = contentEnd;
        } else if (contentEnd < lineEnd && offset < lineEnd) {
            target = lineEnd;
        } else {
            target = offset;
        }
        if (target == offset) {
            return Optional.empty();
        }
        return Optional.of(new MoveCaretToOffsetCommand(target));
    }

    private static int lastNonWhitespaceEnd(String text, int from, int lineEnd) {
        int i = Math.min(lineEnd, text.length());
        while (i > from && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\t')) {
            i--;
        }
        return i;
    }
}
