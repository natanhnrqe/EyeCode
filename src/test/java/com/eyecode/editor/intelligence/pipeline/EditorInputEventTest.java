package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.InputKind;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EditorInputEventTest {

    @Test
    void characterTypedCarriesKindKeyAndCharacter() {
        EditorInputEvent event = EditorInputEvent.characterTyped('(', 3, 7, Set.of());
        assertEquals(InputKind.CHARACTER_TYPED, event.kind());
        assertTrue(event.isCharacterTyped());
        assertEquals("(", event.key());
        assertEquals('(', event.character());
        assertEquals(3, event.offset());
        assertEquals(7, event.documentVersion());
        assertTrue(event.modifiers().isEmpty());
    }

    @Test
    void keyPressedCarriesCanonicalKeyName() {
        EditorInputEvent event = EditorInputEvent.keyPressed("ENTER", 5, 9, Set.of());
        assertEquals(InputKind.KEY_PRESSED, event.kind());
        assertTrue(event.isKeyPressed());
        assertEquals("ENTER", event.key());
        assertEquals('\0', event.character());
    }

    @Test
    void keyReleasedCarriesKind() {
        EditorInputEvent event = EditorInputEvent.keyReleased("BACK_SPACE", 1, 2, Set.of());
        assertEquals(InputKind.KEY_RELEASED, event.kind());
        assertTrue(event.isKeyReleased());
    }

    @Test
    void characterTypedDefaultsSelectionToCaret() {
        EditorInputEvent event = EditorInputEvent.characterTyped('a', 4, 0, Set.of());
        assertEquals(new TextRange(4, 4), event.selection());
    }

    @Test
    void characterTypedCarriesProvidedSelection() {
        EditorInputEvent event = EditorInputEvent.characterTyped('a', 4, 0, Set.of(), new TextRange(2, 6));
        assertEquals(new TextRange(2, 6), event.selection());
    }

    @Test
    void modifiersAreNormalizedToImmutableSet() {
        Set<EditorModifier> supplied = new java.util.HashSet<>();
        supplied.add(EditorModifier.CONTROL);
        EditorInputEvent event = EditorInputEvent.keyPressed("C", 0, 0, supplied);
        assertNotSame(supplied, event.modifiers());
        assertTrue(event.hasModifier(EditorModifier.CONTROL));
        assertFalse(event.hasModifier(EditorModifier.SHIFT));
        assertThrows(UnsupportedOperationException.class, () -> event.modifiers().add(EditorModifier.SHIFT));
    }

    @Test
    void nullModifiersBecomeEmptySet() {
        EditorInputEvent event = EditorInputEvent.characterTyped('x', 0, 0, null);
        assertTrue(event.modifiers().isEmpty());
    }

    @Test
    void enumSetOfModifiersIsAccepted() {
        Set<EditorModifier> modifiers = java.util.EnumSet.of(EditorModifier.SHIFT, EditorModifier.ALT);
        EditorInputEvent event = EditorInputEvent.keyPressed("TAB", 0, 0, modifiers);
        assertEquals(2, event.modifiers().size());
        assertTrue(event.hasModifier(EditorModifier.SHIFT));
        assertTrue(event.hasModifier(EditorModifier.ALT));
    }

    @Test
    void validationRejectsNullKindAndNegativeOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> new EditorInputEvent(null, "x", 'x', Set.of(), 0, 0, new TextRange(0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorInputEvent(InputKind.KEY_PRESSED, "ENTER", '\0', Set.of(), -1, 0, new TextRange(0, 0)));
    }
}
