package com.eyecode.editor.intelligence.selection;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Optional;

/**
 * Computes the selection range that results from expanding the current
 * selection to a given semantic level.
 * <p>
 * Expanders are pure functions: they read the snapshot only and never mutate
 * the document, the caret or the selection. A returned range is guaranteed to
 * be clamped to the snapshot and normalized.
 * <p>
 * Level semantics (Java-first):
 * <ol>
 *   <li>word/token at the caret;</li>
 *   <li>simple expression (dot chains, calls and binary operators);</li>
 *   <li>delimited content (full argument list between delimiters);</li>
 *   <li>the enclosing delimiter pair itself;</li>
 *   <li>logical statement (up to and including the terminating {@code ;});</li>
 *   <li>the enclosing {@code { ... }} block;</li>
 *   <li>the enclosing structural declaration (method/class) or the whole document.</li>
 * </ol>
 * When the current selection cannot grow any further at the requested level the
 * expander returns {@link Optional#empty()}, and the caller falls back to the
 * native behavior.
 */
public interface SelectionExpander {

    int maxLevel();

    Optional<TextRange> expand(DocumentSnapshot snapshot,
                               int caretOffset,
                               Optional<TextRange> selection,
                               int level);
}
