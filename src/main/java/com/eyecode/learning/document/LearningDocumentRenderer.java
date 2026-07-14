package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.DifficultyLevel;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LearningDocumentRenderer {

    private final StyledDocument doc;

    private final SimpleAttributeSet pageTitleStyle;
    private final SimpleAttributeSet metaPrimaryStyle;
    private final SimpleAttributeSet metaSecondaryStyle;
    private final SimpleAttributeSet sectionTitleStyle;
    private final SimpleAttributeSet bodyStyle;
    private final SimpleAttributeSet codeStyle;
    private final SimpleAttributeSet bulletStyle;
    private final SimpleAttributeSet linkStyle;
    private final SimpleAttributeSet arrowStyle;
    private final SimpleAttributeSet dividerStyle;

    public LearningDocumentRenderer() {
        this.doc = new DefaultStyledDocument();
        this.pageTitleStyle = LearningDocumentStyle.title();
        this.metaPrimaryStyle = LearningDocumentStyle.metaPrimary();
        this.metaSecondaryStyle = LearningDocumentStyle.metaSecondary();
        this.sectionTitleStyle = LearningDocumentStyle.sectionTitle();
        this.bodyStyle = LearningDocumentStyle.body();
        this.codeStyle = LearningDocumentStyle.code();
        this.bulletStyle = LearningDocumentStyle.bullet();
        this.linkStyle = LearningDocumentStyle.link();
        this.arrowStyle = LearningDocumentStyle.arrow();
        this.dividerStyle = LearningDocumentStyle.divider();
    }

    public StyledDocument render(LearningPage page) {
        try {
            doc.remove(0, doc.getLength());

            if (page == null) {
                return doc;
            }

            renderPageHeader(page);
            renderDivider();

            List<LearningContentSection> sections = orderedSections(page);
            for (int i = 0; i < sections.size(); i++) {
                renderSection(sections.get(i));
                if (i < sections.size() - 1) {
                    renderDivider();
                }
            }
        } catch (BadLocationException e) {
            // should not happen with append-only document mutations
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

    private void renderPageHeader(LearningPage page) throws BadLocationException {
        append("📘 " + nullToEmpty(page.getTitle()), pageTitleStyle);
        append("\n", bodyStyle);

        append(difficultyLabel(page != null ? page.getDifficulty() : null), metaPrimaryStyle);
        append(" • ", metaSecondaryStyle);
        append(estimateLabel(page), metaSecondaryStyle);
        append("\n", bodyStyle);
    }

    private void renderSection(LearningContentSection section) throws BadLocationException {
        if (section == null) {
            return;
        }

        LearningContentType type = section.getType();
        String content = section.getContent();

        if (type == null) {
            renderGenericSection(section.getTitle(), content);
            return;
        }

        switch (type) {
            case INTRODUCTION -> renderTextSection("💡 O que é isso?", content);
            case ANALOGY -> renderTextSection("🏠 Analogia", content);
            case REAL_WORLD_EXAMPLE -> renderMixedSection("🌎 Onde isso aparece?", content);
            case CODE_EXAMPLE -> renderCodeSection("💻 Exemplo", content);
            case HOW_IT_WORKS -> renderFlowSection("🧠 Como funciona?", content);
            case COMMON_MISTAKES -> renderMistakesSection("⚠ Erros comuns", content);
            case NEXT_STEP -> renderTextSection("➡ Continue estudando", content);
            case TECHNICAL_REFERENCE -> renderReferenceSection("📚 Ver documentação oficial", content);
            case CURIOSITY -> renderTextSection("🚀 Dica", content);
            default -> renderGenericSection(section.getTitle(), content);
        }
    }

    private void renderGenericSection(String title, String content) throws BadLocationException {
        if (hasText(title)) {
            append(title, sectionTitleStyle);
            append("\n", bodyStyle);
        }
        renderNarrative(content);
    }

    private void renderTextSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);
        renderNarrative(content);
    }

    private void renderMixedSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);

        for (String block : splitBlocks(content)) {
            if (isBulletBlock(block)) {
                renderBullets(extractBullets(block));
            } else {
                renderSentences(block);
            }
        }
    }

    private void renderCodeSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);

        List<String> blocks = splitBlocks(content);
        boolean renderedCode = false;
        for (String block : blocks) {
            if (!renderedCode && looksLikeCode(block)) {
                renderCodeBlock(block);
                renderedCode = true;
            } else {
                renderSentences(block);
            }
        }
    }

    private void renderFlowSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);

        boolean renderedNarrative = false;
        for (String block : splitBlocks(content)) {
            if (isBulletBlock(block)) {
                renderBullets(extractBullets(block));
                continue;
            }

            if (!hasText(block)) {
                continue;
            }

            if (renderedNarrative) {
                append("↓", arrowStyle);
                append("\n", bodyStyle);
            }

            renderSentences(block);
            renderedNarrative = true;
        }
    }

    private void renderMistakesSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);

        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }

            if (isNumberedBlock(block)) {
                append("• " + stripNumberPrefix(firstLine(block)), bulletStyle);
                append("\n", bodyStyle);
                renderNarrative(remainderAfterFirstLine(block));
            } else if (isBulletBlock(block)) {
                renderBullets(extractBullets(block));
            } else {
                renderNarrative(block);
            }
        }
    }

    private void renderReferenceSection(String title, String content) throws BadLocationException {
        append(title, sectionTitleStyle);
        append("\n", bodyStyle);
        renderNarrative(content);
    }

    private void renderNarrative(String content) throws BadLocationException {
        for (String block : splitBlocks(content)) {
            renderSentences(block);
        }
    }

    private void renderSentences(String block) throws BadLocationException {
        for (String sentence : splitSentences(block)) {
            append(sentence, bodyStyle);
            append("\n", bodyStyle);
        }
    }

    private void renderCodeBlock(String code) throws BadLocationException {
        append("\n", bodyStyle);
        for (String line : code.split("\n", -1)) {
            append("  " + line + "  ", codeStyle);
            append("\n", codeStyle);
        }
        append("\n", bodyStyle);
    }

    private void renderBullets(List<String> bullets) throws BadLocationException {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }

        for (String bullet : bullets) {
            append("• " + bullet, bulletStyle);
            append("\n", bodyStyle);
        }
    }

    private void renderDivider() throws BadLocationException {
        append(LearningDocumentStyle.dividerText(), dividerStyle);
        append("\n", bodyStyle);
    }

    private static List<LearningContentSection> orderedSections(LearningPage page) {
        if (page == null || page.getSections() == null) {
            return List.of();
        }

        List<LearningContentSection> sections = new ArrayList<>(page.getSections());
        sections.sort(Comparator.comparingInt(section -> section != null ? section.getOrder() : Integer.MAX_VALUE));
        return sections;
    }

    private static List<String> splitBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        if (!hasText(content)) {
            return blocks;
        }

        for (String block : content.split("\\n\\n")) {
            String trimmed = block.trim();
            if (!trimmed.isBlank()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private static List<String> splitSentences(String block) {
        List<String> sentences = new ArrayList<>();
        if (!hasText(block)) {
            return sentences;
        }

        for (String part : block.split("(?<=[.!?])\\s+|\\n+")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private static List<String> extractBullets(String block) {
        List<String> bullets = new ArrayList<>();
        if (!hasText(block)) {
            return bullets;
        }

        for (String line : block.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                bullets.add(trimmed.substring(2).trim());
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                bullets.add(stripNumberPrefix(trimmed));
            }
        }
        return bullets;
    }

    private static boolean isBulletBlock(String block) {
        if (!hasText(block)) {
            return false;
        }

        String trimmed = block.trim();
        return trimmed.startsWith("- ") || trimmed.matches("^(\\d+\\.\\s).*");
    }

    private static boolean isNumberedBlock(String block) {
        return hasText(block) && firstLine(block).trim().matches("^\\d+\\.\\s.*");
    }

    private static boolean looksLikeCode(String block) {
        if (!hasText(block)) {
            return false;
        }

        String trimmed = block.trim();
        return trimmed.contains("class ")
                || trimmed.contains("public ")
                || trimmed.contains("private ")
                || trimmed.contains("void ")
                || trimmed.contains("{")
                || trimmed.contains("}")
                || trimmed.contains("System.out");
    }

    private static String stripNumberPrefix(String text) {
        if (!hasText(text)) {
            return "";
        }
        return text.replaceFirst("^\\d+\\.\\s*", "").trim();
    }

    private static String firstLine(String block) {
        if (!hasText(block)) {
            return "";
        }

        int idx = block.indexOf('\n');
        return idx >= 0 ? block.substring(0, idx) : block;
    }

    private static String remainderAfterFirstLine(String block) {
        if (!hasText(block)) {
            return "";
        }

        int idx = block.indexOf('\n');
        return idx >= 0 ? block.substring(idx + 1).trim() : "";
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private static String nullToEmpty(String text) {
        return text != null ? text : "";
    }

    private static String difficultyLabel(DifficultyLevel difficulty) {
        if (difficulty == null) {
            return "";
        }

        return switch (difficulty) {
            case BEGINNER -> "Iniciante";
            case INTERMEDIATE -> "Intermediário";
            case ADVANCED -> "Avançado";
        };
    }

    private static String estimateLabel(LearningPage page) {
        int sectionCount = page != null && page.getSections() != null ? page.getSections().size() : 0;
        int minutes = Math.max(6, sectionCount * 2);
        return minutes + " min";
    }

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }
}
