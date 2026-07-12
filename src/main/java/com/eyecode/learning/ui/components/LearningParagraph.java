package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.*;

public final class LearningParagraph extends JTextPane {

    private static final int FONT_SIZE = 13;

    public LearningParagraph(String text) {
        super();
        setContentType("text/plain");
        setText(text);
        setFont(TypographyManager.monoRegular(FONT_SIZE));
        setForeground(ColorManager.TEXT_SECONDARY);
        setBackground(new Color(0, 0, 0, 0));
        setEditable(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        setOpaque(false);

        StyledDocument doc = getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(attrs, 0.3f);
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);

        setEditorKit(new StyledEditorKit() {
            @Override
            public ViewFactory getViewFactory() {
                return new WarpViewFactory();
            }
        });
        setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
    }

    private static final class WarpViewFactory implements ViewFactory {
        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                return switch (kind) {
                    case AbstractDocument.ContentElementName -> new LabelView(elem);
                    case AbstractDocument.ParagraphElementName -> new ParagraphView(elem) {
                        @Override
                        protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
                            if (r == null) r = new SizeRequirements();
                            float pref = (int) super.calculateMinorAxisRequirements(axis, r).preferred;
                            r.minimum = (int) pref;
                            r.preferred = Math.max(r.preferred, (int) pref + 1);
                            r.maximum = (int) pref + 1;
                            return r;
                        }
                    };
                    case AbstractDocument.SectionElementName -> new BoxView(elem, View.Y_AXIS) {
                        @Override
                        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
                            super.layoutMajorAxis(targetSpan, axis, offsets, spans);
                        }
                    };
                    case StyleConstants.ComponentElementName -> new ComponentView(elem);
                    case StyleConstants.IconElementName -> new IconView(elem);
                    default -> new LabelView(elem);
                };
            }
            return new LabelView(elem);
        }
    }
}
