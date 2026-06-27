package com.eyecode.editor.v2.syntax.swing;

import com.eyecode.editor.v2.syntax.StyleDefinition;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;

public final class SwingStyleMapper {

    public AttributeSet toAttributes(StyleDefinition definition) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        if (definition == null) return attributes;

        StyleConstants.setBold(attributes, definition.bold());
        StyleConstants.setItalic(attributes, definition.italic());
        Color foreground = resolveColor(definition.foregroundKey());
        if (foreground != null) {
            StyleConstants.setForeground(attributes, foreground);
        }
        return attributes;
    }

    private Color resolveColor(String foregroundKey) {
        if (foregroundKey == null) return ColorManager.EDITOR_FOREGROUND;
        return switch (foregroundKey) {
            case "keyword" -> ColorManager.SYNTAX_KEYWORD;
            case "string" -> ColorManager.SYNTAX_STRING;
            case "comment" -> ColorManager.SYNTAX_COMMENT;
            case "number" -> ColorManager.SYNTAX_NUMBER;
            case "annotation" -> ColorManager.SYNTAX_ANNOTATION;
            case "operator" -> ColorManager.SYNTAX_OPERATOR;
            case "separator" -> ColorManager.SYNTAX_SEPARATOR;
            case "identifier" -> ColorManager.SYNTAX_IDENTIFIER;
            default -> ColorManager.EDITOR_FOREGROUND;
        };
    }
}
