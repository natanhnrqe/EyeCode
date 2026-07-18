package com.eyecode.learning.markdown;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.DifficultyLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MarkdownBuilder {

    public String build(LearningPage page) {
        StringBuilder sb = new StringBuilder();

        if (page == null) {
            return "";
        }

        sb.append("# ").append(nullToEmpty(page.getTitle())).append("\n");

        String difficulty = difficultyLabel(page.getDifficulty());
        String estimate = estimateLabel(page);
        sb.append(difficulty).append(" \u2022 ").append(estimate).append("\n");

        sb.append("---\n");

        List<LearningContentSection> sections = orderedSections(page);
        for (int i = 0; i < sections.size(); i++) {
            LearningContentSection section = sections.get(i);
            if (section != null) {
                renderSection(section, sb);
            }
            if (i < sections.size() - 1) {
                sb.append("---\n");
            }
        }

        return sb.toString().trim();
    }

    private void renderSection(LearningContentSection section, StringBuilder sb) {
        LearningContentType type = section.getType();
        if (type == null) {
            renderGenericSection(section.getTitle(), section.getContent(), sb);
            return;
        }

        String icon = sectionIcon(type);
        String title = sectionTitle(type);
        sb.append("\n## ").append(icon).append(" ").append(title).append("\n");

        String content = section.getContent();
        if (content == null || content.isBlank()) {
            return;
        }

        switch (type) {
            case CODE_EXAMPLE -> renderCodeContent(content, sb);
            case HOW_IT_WORKS -> renderFlowContent(content, sb);
            case COMMON_MISTAKES -> renderMistakesContent(content, sb);
            case TECHNICAL_REFERENCE -> renderReferenceContent(content, sb);
            case CURIOSITY -> sb.append("\n:::tip\n").append(content.trim()).append("\n:::\n");
            case ANALOGY -> sb.append("\n:::info\n").append(content.trim()).append("\n:::\n");
            default -> renderBodyContent(content, sb);
        }
    }

    private void renderGenericSection(String title, String content, StringBuilder sb) {
        if (title != null && !title.isBlank()) {
            sb.append("\n## ").append(title).append("\n");
        }
        renderBodyContent(content, sb);
    }

    private void renderCodeContent(String content, StringBuilder sb) {
        List<String> blocks = splitBlocks(content);
        boolean renderedCode = false;
        for (String block : blocks) {
            if (!renderedCode && looksLikeCode(block)) {
                sb.append("\n```java\n").append(block.trim()).append("\n```\n");
                renderedCode = true;
            } else {
                splitSentencesAsParagraphs(block, sb);
            }
        }
    }

    private void renderFlowContent(String content, StringBuilder sb) {
        boolean renderedNarrative = false;
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isBulletBlock(block)) {
                renderBulletLines(block, sb);
                continue;
            }
            if (renderedNarrative) {
                sb.append("\u2193\n");
            }
            splitSentencesAsParagraphs(block, sb);
            renderedNarrative = true;
        }
    }

    private void renderMistakesContent(String content, StringBuilder sb) {
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isNumberedBlock(block)) {
                sb.append("\n:::warning\n").append(stripNumberPrefix(firstLine(block))).append("\n:::\n");
                String rest = remainderAfterFirstLine(block);
                if (hasText(rest)) {
                    splitSentencesAsParagraphs(rest, sb);
                }
            } else if (isBulletBlock(block)) {
                renderMistakeBullets(block, sb);
            } else {
                splitSentencesAsParagraphs(block, sb);
            }
        }
    }

    private void renderReferenceContent(String content, StringBuilder sb) {
        for (String sentence : splitSentences(content)) {
            sb.append(sentence).append("\n");
        }
    }

    private void renderBodyContent(String content, StringBuilder sb) {
        for (String block : splitBlocks(content)) {
            if (!hasText(block)) {
                continue;
            }
            if (isBulletBlock(block)) {
                renderBulletLines(block, sb);
            } else {
                splitSentencesAsParagraphs(block, sb);
            }
        }
    }

    private void renderBulletLines(String block, StringBuilder sb) {
        for (String line : block.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                sb.append("- ").append(trimmed.substring(2).trim()).append("\n");
            } else if (trimmed.startsWith("\u2022 ")) {
                sb.append("- ").append(trimmed.substring(2).trim()).append("\n");
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                sb.append("- ").append(stripNumberPrefix(trimmed)).append("\n");
            }
        }
    }

    private void renderMistakeBullets(String block, StringBuilder sb) {
        for (String line : block.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("\u2022 ")) {
                String bulletText = trimmed.startsWith("- ")
                        ? trimmed.substring(2).trim()
                        : trimmed.substring(2).trim();
                sb.append("\n:::warning\n").append(bulletText).append("\n:::\n");
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                sb.append("\n:::warning\n").append(stripNumberPrefix(trimmed)).append("\n:::\n");
            }
        }
    }

    private void splitSentencesAsParagraphs(String block, StringBuilder sb) {
        if (!hasText(block)) {
            return;
        }
        for (String sentence : splitSentences(block)) {
            sb.append(sentence).append("\n\n");
        }
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
