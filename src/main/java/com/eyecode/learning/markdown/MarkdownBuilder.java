package com.eyecode.learning.markdown;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.DifficultyLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownBuilder {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    public MarkdownDocument build(LearningPage page) {
        List<MarkdownNode> nodes = new ArrayList<>();

        if (page == null) {
            return new MarkdownDocument(nodes);
        }

        nodes.add(new HeadingNode(1, nullToEmpty(page.getTitle())));

        String difficulty = difficultyLabel(page.getDifficulty());
        String estimate = estimateLabel(page);
        nodes.add(new ParagraphNode(List.of(
                Segment.text(difficulty),
                Segment.text(" \u2022 "),
                Segment.text(estimate)
        )));

        nodes.add(new DividerNode());

        List<LearningContentSection> sections = orderedSections(page);
        for (int i = 0; i < sections.size(); i++) {
            LearningContentSection section = sections.get(i);
            if (section != null) {
                renderSection(section, nodes);
            }
            if (i < sections.size() - 1) {
                nodes.add(new DividerNode());
            }
        }

        return new MarkdownDocument(nodes);
    }

    private void renderSection(LearningContentSection section, List<MarkdownNode> nodes) {
        LearningContentType type = section.getType();
        if (type == null) {
            renderGenericSection(section.getTitle(), section.getContent(), nodes);
            return;
        }

        String icon = sectionIcon(type);
        String title = sectionTitle(type);
        String heading = icon + " " + title;
        nodes.add(new HeadingNode(2, heading));

        String content = section.getContent();
        if (content == null || content.isBlank()) {
            return;
        }

        switch (type) {
            case CODE_EXAMPLE -> renderCodeContent(content, nodes);
            case HOW_IT_WORKS -> renderFlowContent(content, nodes);
            case COMMON_MISTAKES -> renderMistakesContent(content, nodes);
            case TECHNICAL_REFERENCE -> renderReferenceContent(content, nodes);
            default -> renderBodyContent(content, nodes);
        }
    }

    private void renderGenericSection(String title, String content, List<MarkdownNode> nodes) {
        if (title != null && !title.isBlank()) {
            nodes.add(new HeadingNode(2, title));
        }
        renderBodyContent(content, nodes);
    }

    private void renderCodeContent(String content, List<MarkdownNode> nodes) {
        List<String> blocks = splitBlocks(content);
        boolean renderedCode = false;
        for (String block : blocks) {
            if (!renderedCode && looksLikeCode(block)) {
                nodes.add(new CodeBlockNode("java", block.trim()));
                renderedCode = true;
            } else {
                nodes.addAll(splitSentencesAsParagraphs(block));
            }
        }
    }

    private void renderFlowContent(String content, List<MarkdownNode> nodes) {
        boolean renderedNarrative = false;
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isBulletBlock(block)) {
                renderBulletLines(block, nodes);
                continue;
            }
            if (renderedNarrative) {
                nodes.add(new ParagraphNode(List.of(Segment.text("\u2193"))));
            }
            nodes.addAll(splitSentencesAsParagraphs(block));
            renderedNarrative = true;
        }
    }

    private void renderMistakesContent(String content, List<MarkdownNode> nodes) {
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isNumberedBlock(block)) {
                String mistakeText = "\u274C " + stripNumberPrefix(firstLine(block));
                nodes.add(new ParagraphNode(parseInline(mistakeText)));
                String rest = remainderAfterFirstLine(block);
                if (hasText(rest)) {
                    nodes.addAll(splitSentencesAsParagraphs(rest));
                }
            } else if (isBulletBlock(block)) {
                renderMistakeBullets(block, nodes);
            } else {
                nodes.addAll(splitSentencesAsParagraphs(block));
            }
        }
    }

    private void renderReferenceContent(String content, List<MarkdownNode> nodes) {
        for (String sentence : splitSentences(content)) {
            nodes.add(new ParagraphNode(parseInline(sentence)));
        }
    }

    private void renderBodyContent(String content, List<MarkdownNode> nodes) {
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isBulletBlock(block)) {
                renderBulletLines(block, nodes);
            } else {
                nodes.addAll(splitSentencesAsParagraphs(block));
            }
        }
    }

    private void renderBulletLines(String block, List<MarkdownNode> nodes) {
        for (String line : block.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                nodes.add(new BulletNode(trimmed.substring(2).trim(), false));
            } else if (trimmed.startsWith("\u2022 ")) {
                nodes.add(new BulletNode(trimmed.substring(2).trim(), false));
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                nodes.add(new BulletNode(stripNumberPrefix(trimmed), false));
            }
        }
    }

    private void renderMistakeBullets(String block, List<MarkdownNode> nodes) {
        for (String line : block.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("\u2022 ")) {
                String bulletText = trimmed.startsWith("- ")
                        ? trimmed.substring(2).trim()
                        : trimmed.substring(2).trim();
                nodes.add(new ParagraphNode(parseInline("\u274C " + bulletText)));
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                nodes.add(new ParagraphNode(parseInline("\u274C " + stripNumberPrefix(trimmed))));
            }
        }
    }

    private List<MarkdownNode> splitSentencesAsParagraphs(String block) {
        List<MarkdownNode> result = new ArrayList<>();
        if (!hasText(block)) {
            return result;
        }
        for (String sentence : splitSentences(block)) {
            result.add(new ParagraphNode(parseInline(sentence)));
        }
        return result;
    }

    static List<Segment> parseInline(String text) {
        if (text == null || text.isBlank()) {
            return List.of(Segment.text(text != null ? text : ""));
        }

        List<Segment> segments = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            Matcher linkMatcher = LINK_PATTERN.matcher(text).region(pos, text.length());
            if (linkMatcher.find() && linkMatcher.start() == pos) {
                segments.add(Segment.link(linkMatcher.group(1), linkMatcher.group(2)));
                pos = linkMatcher.end();
                continue;
            }

            Matcher boldMatcher = BOLD_PATTERN.matcher(text).region(pos, text.length());
            if (boldMatcher.find() && boldMatcher.start() == pos) {
                segments.add(Segment.bold(boldMatcher.group(1)));
                pos = boldMatcher.end();
                continue;
            }

            Matcher italicMatcher = ITALIC_PATTERN.matcher(text).region(pos, text.length());
            if (italicMatcher.find() && italicMatcher.start() == pos) {
                segments.add(Segment.italic(italicMatcher.group(1)));
                pos = italicMatcher.end();
                continue;
            }

            Matcher codeMatcher = CODE_PATTERN.matcher(text).region(pos, text.length());
            if (codeMatcher.find() && codeMatcher.start() == pos) {
                segments.add(Segment.code(codeMatcher.group(1)));
                pos = codeMatcher.end();
                continue;
            }

            int nextPos = findNextSpecial(text, pos + 1);
            if (nextPos < 0) {
                segments.add(Segment.text(text.substring(pos)));
                pos = text.length();
            } else {
                segments.add(Segment.text(text.substring(pos, nextPos)));
                pos = nextPos;
            }
        }

        return segments;
    }

    private static int findNextSpecial(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '*' || c == '`' || c == '[') {
                return i;
            }
        }
        return -1;
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

    private static boolean isBulletBlock(String block) {
        if (!hasText(block)) {
            return false;
        }
        String trimmed = block.trim();
        return trimmed.startsWith("- ") || trimmed.startsWith("\u2022 ")
                || trimmed.matches("^(\\d+\\.\\s).*");
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
            case INTERMEDIATE -> "Intermedi\u00E1rio";
            case ADVANCED -> "Avan\u00E7ado";
        };
    }

    private static String estimateLabel(LearningPage page) {
        int sectionCount = page != null && page.getSections() != null
                ? page.getSections().size()
                : 0;
        int minutes = Math.max(6, sectionCount * 2);
        return minutes + " min";
    }

    private static String sectionIcon(LearningContentType type) {
        return switch (type) {
            case INTRODUCTION -> "\uD83D\uDCA1";
            case ANALOGY -> "\uD83C\uDFE0";
            case REAL_WORLD_EXAMPLE -> "\uD83C\uDF0E";
            case CODE_EXAMPLE -> "\uD83D\uDCBB";
            case HOW_IT_WORKS -> "\uD83E\uDDE0";
            case COMMON_MISTAKES -> "\u26A0\uFE0F";
            case CURIOSITY -> "\uD83D\uDE80";
            case NEXT_STEP -> "\u27A1\uFE0F";
            case TECHNICAL_REFERENCE -> "\uD83D\uDCDA";
        };
    }

    private static String sectionTitle(LearningContentType type) {
        return switch (type) {
            case INTRODUCTION -> "O que \u00E9 isso?";
            case ANALOGY -> "Analogia";
            case REAL_WORLD_EXAMPLE -> "Onde isso aparece?";
            case CODE_EXAMPLE -> "Exemplo";
            case HOW_IT_WORKS -> "Como funciona?";
            case COMMON_MISTAKES -> "Erros comuns";
            case CURIOSITY -> "Dica";
            case NEXT_STEP -> "Pr\u00F3ximo passo";
            case TECHNICAL_REFERENCE -> "Ver documenta\u00E7\u00E3o oficial";
        };
    }

    private static List<LearningContentSection> orderedSections(LearningPage page) {
        if (page == null || page.getSections() == null) {
            return List.of();
        }
        List<LearningContentSection> sections = new ArrayList<>(page.getSections());
        sections.sort(Comparator.comparingInt(
                section -> section != null ? section.getOrder() : Integer.MAX_VALUE));
        return sections;
    }
}
