package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;

import javax.swing.text.*;
import java.util.ArrayList;
import java.util.List;

public final class LearningDocumentRenderer {

    private static final int SECTION_SPACING = 8;

    private final StyledDocument doc;

     private final SimpleAttributeSet titleStyle;
    private final SimpleAttributeSet subtitleStyle;
    private final SimpleAttributeSet sectionTitleStyle;
    private final SimpleAttributeSet bodyStyle;
    private final SimpleAttributeSet codeStyle;
    private final SimpleAttributeSet bulletStyle;
    private final SimpleAttributeSet dividerLeftStyle;
    private final SimpleAttributeSet dividerRightStyle;

    public LearningDocumentRenderer() {
        doc = new DefaultStyledDocument();
        titleStyle = LearningDocumentStyle.title();
        subtitleStyle = LearningDocumentStyle.subtitle();
        sectionTitleStyle = LearningDocumentStyle.sectionTitle();
        bodyStyle = LearningDocumentStyle.body();
        codeStyle = LearningDocumentStyle.code();
        bulletStyle = LearningDocumentStyle.bullet();
        dividerLeftStyle = LearningDocumentStyle.dividerLeft();
        dividerRightStyle = LearningDocumentStyle.dividerRight();
        configureDocument(doc);
    }

    public StyledDocument render(LearningPage page) {
        try {
            doc.remove(0, doc.getLength());

            if (page == null || page.getSections() == null) return doc;

            List<LearningContentSection> sections = page.getSections();
            for (int i = 0; i < sections.size(); i++) {
                LearningContentSection section = sections.get(i);
                renderSection(section);

                if (i < sections.size() - 1) {
                    renderDivider();
                }
            }
        } catch (BadLocationException e) {
            // should not happen
        }

        return doc;
    }

    public StyledDocument getDocument() {
        return doc;
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // should not happen
        }
    }

    private void renderSection(LearningContentSection section) throws BadLocationException {
        int len = doc.getLength();

        append("\n", bodyStyle);
        append(section.getTitle(), sectionTitleStyle);
        append("\n", bodyStyle);

        String content = section.getContent();
        LearningContentType type = section.getType();

        switch (type) {
            case CODE_EXAMPLE:
                renderCode(content);
                break;
            case COMMON_MISTAKES:
                renderBullets(parseBullets(content));
                break;
            case REAL_WORLD_EXAMPLE:
                if (content.contains("\n- ")) {
                    renderBullets(parseBullets(content));
                } else {
                    renderParagraphs(content);
                }
                break;
            default:
                renderParagraphs(content);
                break;
        }
    }

    private void renderParagraphs(String content) throws BadLocationException {
        if (content == null || content.isBlank()) return;

        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isBlank()) continue;
            append(trimmed, bodyStyle);
            append("\n\n", bodyStyle);
        }
    }

    private void renderCode(String code) throws BadLocationException {
        if (code == null || code.isBlank()) return;
        append(code, codeStyle);
        append("\n", bodyStyle);
    }

    private void renderBullets(List<String> bullets) throws BadLocationException {
        if (bullets == null || bullets.isEmpty()) return;

        for (String bullet : bullets) {
            append("\u2022 " + bullet, bulletStyle);
            append("\n", bulletStyle);
        }
    }

    private void renderDivider() throws BadLocationException {
        append("\n", bodyStyle);
        append(" ", dividerLeftStyle);
        append("\n", bodyStyle);
    }

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }

    private static void configureDocument(StyledDocument doc) {
        StyleContext context = StyleContext.getDefaultStyleContext();
        SimpleAttributeSet defaultAttrs = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(defaultAttrs, 0.0f);
        StyleConstants.setRightIndent(defaultAttrs, 0.0f);
        StyleConstants.setFirstLineIndent(defaultAttrs, 0.0f);
        doc.setParagraphAttributes(0, doc.getLength(), defaultAttrs, true);
    }

    private static List<String> parseBullets(String content) {
        List<String> bullets = new ArrayList<>();
        if (content == null || content.isBlank()) return bullets;

        String[] parts = content.split("\n\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.startsWith("- ")) {
                bullets.add(trimmed.substring(2).trim());
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                bullets.add(trimmed.replaceFirst("^\\d+\\.\\s*", "").trim());
            } else if (!bullets.isEmpty() || trimmed.startsWith("1.")) {
                bullets.add(trimmed);
            }
        }
        return bullets;
    }
}
