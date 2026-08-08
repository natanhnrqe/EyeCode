package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.indent.IndentContext;
import com.eyecode.editor.intelligence.indent.IndentPolicy;
import com.eyecode.editor.intelligence.indent.JavaIndentPolicy;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Claims plain Enter key presses at the highest priority and implements the
 * brace-aware "smart enter" cases:
 * <ul>
 *   <li>an active selection is replaced by a newline plus indentation;</li>
 *   <li>{@code {|}} between a same-line brace pair is split into three lines;</li>
 *   <li>Enter right after a {@code {} that ends its line inserts one level of
 *       indentation (an existing closing brace below keeps its line);</li>
 *   <li>Enter on a blank line that precedes a closing brace normalizes the
 *       indentation and lets the new line take the block level.</li>
 * </ul>
 * Nesting levels come from the {@link IndentContext} scan, so braces inside
 * strings, characters and comments never affect the layout. Cases that do not
 * match yield no command and fall through to {@link AutoIndentStrategy}.
 */
public final class SmartEnterStrategy implements SmartEditStrategy {

    private final IndentPolicy policy;

    public SmartEnterStrategy() {
        this(JavaIndentPolicy.INSTANCE);
    }

    public SmartEnterStrategy(IndentPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        this.policy = policy;
    }

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.HIGH;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainKeyPressed(event, "ENTER");
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        int offset = event.offset();
        TextRange selection = event.selection();
        String text = snapshot.getText();
        LineMap map = snapshot.lineMap();
        int line = map.lineOfOffset(offset);

        if (selection != null && !selection.isEmpty()) {
            int level = policy.nextLineIndentLevel(snapshot, line);
            return Optional.of(new InsertNewlineCommand(offset, selection, policy.indentationFor(level)));
        }

        IndentContext indent = IndentContext.of(snapshot);
        int nesting = Math.max(0, indent.blockDepthAtLineStart(line));

        boolean beforeIsOpenBrace = offset > 0 && offset <= text.length() && text.charAt(offset - 1) == '{';
        boolean afterIsCloseBrace = offset < text.length() && text.charAt(offset) == '}';

        if (beforeIsOpenBrace && afterIsCloseBrace) {
            String mid = policy.indentationFor(nesting + 1);
            String close = policy.indentationFor(nesting);
            return Optional.of(new SmartEnterCommand(offset, null,
                    "\n" + mid + "\n" + close, offset + 1 + mid.length()));
        }

        int lineEnd = map.lineEndOffset(line);
        if (beforeIsOpenBrace && restOfLineIsBlank(text, offset, lineEnd)) {
            String mid = policy.indentationFor(nesting + 1);
            return Optional.of(new SmartEnterCommand(offset, null, "\n" + mid, offset + 1 + mid.length()));
        }

        int lineStart = map.lineStartOffset(line);
        if (prefixIsWhitespace(text, lineStart, offset) && restIsStandaloneClosingBrace(text, offset, lineEnd)) {
            String mid = policy.indentationFor(nesting);
            String close = policy.indentationFor(nesting - 1);
            return Optional.of(new SmartEnterCommand(lineStart, new TextRange(lineStart, offset),
                    mid + "\n" + close, lineStart + mid.length()));
        }

        if (offset == lineEnd && prefixIsWhitespace(text, lineStart, offset)
                && line + 1 < map.lineCount() && indent.lineStartsWithClosingBrace(line + 1)) {
            String mid = policy.indentationFor(nesting);
            return Optional.of(new SmartEnterCommand(offset, null, "\n" + mid, offset + 1 + mid.length()));
        }

        return Optional.empty();
    }

    private static boolean restOfLineIsBlank(String text, int from, int lineEnd) {
        for (int i = Math.max(0, Math.min(from, text.length())); i < lineEnd; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean prefixIsWhitespace(String text, int lineStart, int offset) {
        for (int i = Math.max(0, Math.min(lineStart, text.length())); i < offset; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean restIsStandaloneClosingBrace(String text, int from, int lineEnd) {
        int i = Math.max(0, Math.min(from, text.length()));
        while (i < lineEnd && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        if (i >= lineEnd || text.charAt(i) != '}') {
            return false;
        }
        i++;
        while (i < lineEnd && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        if (i >= lineEnd) {
            return true;
        }
        return text.charAt(i) == '/' && i + 1 < lineEnd && text.charAt(i + 1) == '/';
    }
}
