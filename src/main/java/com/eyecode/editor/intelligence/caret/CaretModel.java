package com.eyecode.editor.intelligence.caret;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Optional;

/**
 * Offset-based caret + selection model of the editor Core.
 * <p>
 * The model is the single reading/writing point for caret and selection state:
 * smart editing strategies, selection expanders and UI adapters all talk to
 * this abstraction instead of touching editor-specific position types.
 * <p>
 * Invariants enforced by implementations:
 * <ul>
 *   <li>offsets are always clamped to {@code [0, documentLength()]};</li>
 *   <li>a selection is always normalized ({@code selectionStart <= selectionEnd});</li>
 *   <li>a zero-length selection means "no selection" ({@link #hasSelection()} is false);</li>
 *   <li>{@link #moveTo(int)} clears the selection, {@link #moveTo(int, boolean)}
 *       keeps it when requested.</li>
 * </ul>
 * This interface is UI-free: it never references Swing, JavaFX or AWT.
 */
public interface CaretModel {

    int offset();

    boolean hasSelection();

    int selectionStart();

    int selectionEnd();

    Optional<TextRange> selection();

    void moveTo(int offset);

    void moveTo(int offset, boolean keepSelection);

    void setSelection(TextRange selection);

    void clearSelection();

    void selectAll();

    int documentLength();
}
