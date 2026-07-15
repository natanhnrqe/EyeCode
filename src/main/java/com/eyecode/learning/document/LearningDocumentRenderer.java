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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LearningDocumentRenderer {

    private static final Pattern KEYWORD_PATTERN = Pattern.compile("\\b(?:class|interface|enum|record|public|private|protected|static|final|void|new|return)\\b");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(?:String|Integer|Boolean|Object|Pessoa|Cliente)\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*");

    private final StyledDocument doc;

    private final SimpleAttributeSet pageTitleStyle;
    private final SimpleAttributeSet metaPrimaryStyle;
    private final SimpleAttributeSet metaSecondaryStyle;
    private final SimpleAttributeSet sectionTitleStyle;
    private final SimpleAttributeSet explanationTitleStyle;
    private final SimpleAttributeSet analogyTitleStyle;
    private final SimpleAttributeSet worldTitleStyle;
    private final SimpleAttributeSet codeTitleStyle;
    private final SimpleAttributeSet flowTitleStyle;
    private final SimpleAttributeSet mistakesTitleStyle;
    private final SimpleAttributeSet nextStepTitleStyle;
    private final SimpleAttributeSet referenceTitleStyle;
    private final SimpleAttributeSet bodyStyle;
    private final SimpleAttributeSet codeStyle;
    private final SimpleAttributeSet bulletStyle;
    private final SimpleAttributeSet mistakeStyle;
    private final SimpleAttributeSet linkStyle;
    private final SimpleAttributeSet arrowStyle;
    private final SimpleAttributeSet dividerStyle;
    private final SimpleAttributeSet codeKeywordStyle;
    private final SimpleAttributeSet codeTypeStyle;
    private final SimpleAttributeSet codeStringStyle;
    private final SimpleAttributeSet codeCommentStyle;

    public LearningDocumentRenderer() {
        this.doc = new DefaultStyledDocument();
        this.pageTitleStyle = LearningDocumentStyle.title();
        this.metaPrimaryStyle = LearningDocumentStyle.metaPrimary();
        this.metaSecondaryStyle = LearningDocumentStyle.metaSecondary();
        this.sectionTitleStyle = LearningDocumentStyle.sectionTitle();
        this.explanationTitleStyle = LearningDocumentStyle.explanationTitle();
        this.analogyTitleStyle = LearningDocumentStyle.analogyTitle();
        this.worldTitleStyle = LearningDocumentStyle.worldTitle();
        this.codeTitleStyle = LearningDocumentStyle.codeTitle();
        this.flowTitleStyle = LearningDocumentStyle.flowTitle();
        this.mistakesTitleStyle = LearningDocumentStyle.mistakesTitle();
        this.nextStepTitleStyle = LearningDocumentStyle.nextStepTitle();
        this.referenceTitleStyle = LearningDocumentStyle.referenceTitle();
        this.bodyStyle = LearningDocumentStyle.body();
        this.codeStyle = LearningDocumentStyle.code();
        this.bulletStyle = LearningDocumentStyle.bullet();
        this.mistakeStyle = LearningDocumentStyle.mistake();
        this.linkStyle = LearningDocumentStyle.link();
        this.arrowStyle = LearningDocumentStyle.arrow();
        this.dividerStyle = LearningDocumentStyle.divider();
        this.codeKeywordStyle = LearningDocumentStyle.codeKeyword();
        this.codeTypeStyle = LearningDocumentStyle.codeType();
        this.codeStringStyle = LearningDocumentStyle.codeString();
        this.codeCommentStyle = LearningDocumentStyle.codeComment();
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
            case INTRODUCTION -> renderTextSection("💡 O que é isso?", content, explanationTitleStyle);
            case ANALOGY -> renderTextSection("🏠 Analogia", content, analogyTitleStyle);
            case REAL_WORLD_EXAMPLE -> renderMixedSection("🌎 Onde isso aparece?", content, worldTitleStyle);
            case CODE_EXAMPLE -> renderCodeSection("💻 Exemplo", content, codeTitleStyle);
            case HOW_IT_WORKS -> renderFlowSection("🧠 Como funciona?", content, flowTitleStyle);
            case COMMON_MISTAKES -> renderMistakesSection("⚠ Erros comuns", content, mistakesTitleStyle);
            case NEXT_STEP -> renderTextSection("➡ Próximo passo", content, nextStepTitleStyle);
            case TECHNICAL_REFERENCE -> renderReferenceSection("📚 Ver documentação oficial", content, referenceTitleStyle);
            case CURIOSITY -> renderTextSection("🚀 Dica", content, sectionTitleStyle);
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

    private void renderTextSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
        append("\n", bodyStyle);
        renderNarrative(content);
    }

    private void renderMixedSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
        append("\n", bodyStyle);

        for (String block : splitBlocks(content)) {
            if (isBulletBlock(block)) {
                renderBullets(extractBullets(block));
            } else {
                renderSentences(block);
            }
        }
    }

    private void renderCodeSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
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

    private void renderFlowSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
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
                append("\u2193", arrowStyle);
                append("\n", bodyStyle);
            }

            renderSentences(block);
            renderedNarrative = true;
        }
    }

    private void renderMistakesSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
        append("\n", bodyStyle);

        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }

            if (isNumberedBlock(block)) {
                append("\u274C " + stripNumberPrefix(firstLine(block)), mistakeStyle);
                append("\n", bodyStyle);
                renderNarrative(remainderAfterFirstLine(block));
            } else if (isBulletBlock(block)) {
                renderMistakeBullets(extractBullets(block));
            } else {
                renderNarrative(block);
            }
        }
    }

    private void renderReferenceSection(String title, String content, SimpleAttributeSet titleStyle) throws BadLocationException {
        append(title, titleStyle);
        append("\n", bodyStyle);
        for (String sentence : splitSentences(content)) {
            append(sentence, linkStyle);
            append("\n", bodyStyle);
        }
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
        int padding = LearningDocumentStyle.codeBlockHorizontalPadding();
        String horizontalPadding = " ".repeat(padding);
        int blockWidth = Math.max(longestLineLength(code), LearningDocumentStyle.codeBlockMinimumColumns());

        for (String line : code.split("\n", -1)) {
            int lineStart = doc.getLength();
            append(horizontalPadding + padRight(line, blockWidth) + horizontalPadding, codeStyle);
            applyCodeSyntax(lineStart + padding, line);
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

    private void renderMistakeBullets(List<String> bullets) throws BadLocationException {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }

        for (String bullet : bullets) {
            append("\u274C " + bullet, mistakeStyle);
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
            } else if (trimmed.startsWith("• ")) {
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
        return trimmed.startsWith("- ") || trimmed.startsWith("• ") || trimmed.matches("^(\\d+\\.\\s).*");
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

    private void applyCodeSyntax(int offset, String line) {
        boolean[] protectedRanges = new boolean[line.length()];
        applyProtectedPattern(offset, line, STRING_PATTERN, codeStringStyle, protectedRanges);
        applyProtectedPattern(offset, line, COMMENT_PATTERN, codeCommentStyle, protectedRanges);
        applyTokenPattern(offset, line, KEYWORD_PATTERN, codeKeywordStyle, protectedRanges);
        applyTokenPattern(offset, line, TYPE_PATTERN, codeTypeStyle, protectedRanges);
    }

    private void applyProtectedPattern(
            int offset,
            String line,
            Pattern pattern,
            SimpleAttributeSet style,
            boolean[] protectedRanges) {

        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (overlapsProtectedRange(matcher.start(), matcher.end(), protectedRanges)) {
                continue;
            }
            doc.setCharacterAttributes(offset + matcher.start(), matcher.end() - matcher.start(), style, false);
            markProtectedRange(matcher.start(), matcher.end(), protectedRanges);
        }
    }

    private void applyTokenPattern(
            int offset,
            String line,
            Pattern pattern,
            SimpleAttributeSet style,
            boolean[] protectedRanges) {

        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (!overlapsProtectedRange(matcher.start(), matcher.end(), protectedRanges)) {
                doc.setCharacterAttributes(offset + matcher.start(), matcher.end() - matcher.start(), style, false);
            }
        }
    }

    private static boolean overlapsProtectedRange(int start, int end, boolean[] protectedRanges) {
        for (int index = start; index < end; index++) {
            if (protectedRanges[index]) {
                return true;
            }
        }
        return false;
    }

    private static void markProtectedRange(int start, int end, boolean[] protectedRanges) {
        for (int index = start; index < end; index++) {
            protectedRanges[index] = true;
        }
    }

    private static int longestLineLength(String text) {
        int longest = 0;
        for (String line : text.split("\n", -1)) {
            longest = Math.max(longest, line.length());
        }
        return longest;
    }

    private static String padRight(String text, int length) {
        return text + " ".repeat(Math.max(0, length - text.length()));
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
