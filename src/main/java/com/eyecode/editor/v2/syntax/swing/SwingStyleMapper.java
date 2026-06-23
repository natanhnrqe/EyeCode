package com.eyecode.editor.v2.syntax.swing;

import com.eyecode.editor.v2.syntax.StyleDefinition;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public final class SwingStyleMapper {

    public AttributeSet toAttributes(StyleDefinition definition) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        if (definition == null) return attributes;

        StyleConstants.setBold(attributes, definition.bold());
        StyleConstants.setItalic(attributes, definition.italic());
        return attributes;
    }
}
