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
 * Smart HOME navigation (IntelliJ-style): a HOME key press alternates between
 * the first non-whitespace character of the line and column 0.
 * <ul>
 *   <li>caret after the first non-whitespace character → first non-whitespace;</li>
 *   <li>caret already on the first non-whitespace character → column 0;</li>
 *   <li>caret inside the leading whitespace → first non-whitespace;</li>
 *   <li>whitespace-only or empty line → column 0.</li>
 * </ul>
 * Only plain HOME presses are claimed (Shift+Home, Ctrl+Home and friends keep
 * their native behavior). The strategy never alters text: it only produces a
 * caret movement, so no undo entry is created.
 */
public final class SmartHomeStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainKeyPressed(event, "HOME", true);
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
        int firstNonWhitespace = IndentLineCommand.leadingWhitespaceEnd(text, lineStart);

        int target;
        if (firstNonWhitespace >= lineEnd) {
            target = lineStart;
        } else if (offset < firstNonWhitespace) {
            target = firstNonWhitespace;
        } else if (offset == firstNonWhitespace) {
            target = lineStart;
        } else {
            target = firstNonWhitespace;
        }
        if (target == offset) {
            return Optional.empty();
        }
        return Optional.of(new MoveCaretToOffsetCommand(target));
    }
}
