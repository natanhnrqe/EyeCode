package com.eyecode.language.java.doc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavadocFormatter {

    private static final String LOOSE_KEY = "";
    private static final Pattern BLOCK_TAG = Pattern.compile("^@(\\w+)\\b(.*)$");
    private static final Pattern CODE = Pattern.compile("\\{@code\\s*(.*?)\\}", Pattern.DOTALL);
    private static final Pattern LINK = Pattern.compile("\\{@(?:link|linkplain)\\s+(.*?)\\}", Pattern.DOTALL);
    private static final Pattern PRE = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_ELEMENT = Pattern.compile("<code>(.*?)</code>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*\"([^\"]*)\"[^>]*>(.*?)</a>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("</?[a-zA-Z][^>]*>");
    private static final Pattern PLACEHOLDER = Pattern.compile("\uE000(\\d+)\uE001");

    private JavadocFormatter() {
    }

    public static String format(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return "";
        }
        String body = javadoc.startsWith("/**") ? LocalJavadocResolver.clean(javadoc) : javadoc;
        List<String> description = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        Block current = null;
        for (String line : body.split("\n", -1)) {
            Matcher matcher = BLOCK_TAG.matcher(line.trim());
            if (matcher.matches()) {
                current = new Block(matcher.group(1).toLowerCase());
                current.lines.add(matcher.group(2).trim());
                blocks.add(current);
            } else if (current == null) {
                description.add(line);
            } else {
                current.lines.add(line);
            }
        }
        return render(description, blocks);
    }

    private static String render(List<String> description, List<Block> blocks) {
        Map<String, List<Block>> groups = new LinkedHashMap<>();
        for (Block block : blocks) {
            String key = headingFor(block.tag);
            groups.computeIfAbsent(key == null ? LOOSE_KEY : key, ignored -> new ArrayList<>()).add(block);
        }
        StringBuilder out = new StringBuilder();
        String text = inline(String.join("\n", description));
        if (!text.isBlank()) {
            out.append(text).append("\n\n");
        }
        for (Map.Entry<String, List<Block>> entry : groups.entrySet()) {
            String body = entry.getKey().isEmpty()
                    ? looseBody(entry.getValue())
                    : sectionBody(entry.getKey(), entry.getValue());
            if (body.isBlank()) {
                continue;
            }
            if (!entry.getKey().isEmpty()) {
                out.append(entry.getKey()).append("\n\n");
            }
            out.append(body).append("\n\n");
        }
        return out.toString().trim().replaceAll("\\n{3,}", "\n\n");
    }

    private static String looseBody(List<Block> blocks) {
        List<String> paragraphs = new ArrayList<>();
        for (Block block : blocks) {
            String paragraph = inline(String.join("\n", block.lines));
            if (!paragraph.isBlank()) {
                paragraphs.add(paragraph);
            }
        }
        return String.join("\n\n", paragraphs);
    }

    private static String sectionBody(String heading, List<Block> blocks) {
        if ("## Parâmetros".equals(heading) || "## Exceções".equals(heading)) {
            List<String> items = new ArrayList<>();
            for (Block block : blocks) {
                String item = taggedItem(block);
                if (!item.isBlank()) {
                    items.add(item);
                }
            }
            return String.join("\n", items);
        }
        if ("## Ver também".equals(heading)) {
            List<String> items = new ArrayList<>();
            for (Block block : blocks) {
                String item = seeItem(block);
                if (!item.isBlank()) {
                    items.add(item);
                }
            }
            return String.join("\n", items);
        }
        List<String> paragraphs = new ArrayList<>();
        for (Block block : blocks) {
            String paragraph = inline(String.join("\n", block.lines));
            if (!paragraph.isBlank()) {
                paragraphs.add(paragraph);
            }
        }
        return String.join("\n\n", paragraphs);
    }

    private static String headingFor(String tag) {
        return switch (tag) {
            case "param" -> "## Parâmetros";
            case "throws", "exception" -> "## Exceções";
            case "see" -> "## Ver também";
            case "return", "returns" -> "## Retorno";
            case "deprecated" -> "## Obsoleto";
            case "since" -> "## Desde";
            case "author" -> "## Autor";
            case "version" -> "## Versão";
            case "apinote" -> "## Nota de API";
            case "implnote" -> "## Nota de implementação";
            case "implspec" -> "## Especificação de implementação";
            default -> null;
        };
    }

    private static String taggedItem(Block block) {
        String first = block.lines.isEmpty() ? "" : block.lines.getFirst().trim();
        int space = first.indexOf(' ');
        String name = space < 0 ? first : first.substring(0, space);
        String rest = space < 0 ? "" : first.substring(space + 1).trim();
        List<String> continuation = block.lines.size() > 1
                ? block.lines.subList(1, block.lines.size()) : List.<String>of();
        String joined = join(rest, continuation);
        String description = inline(joined);
        if (name.isBlank()) {
            return description;
        }
        if (description.isBlank()) {
            return "- **" + name + "**";
        }
        return "- **" + name + "** — " + description;
    }

    private static String join(String first, List<String> continuation) {
        List<String> lines = new ArrayList<>();
        if (!first.isBlank()) {
            lines.add(first);
        }
        for (String line : continuation) {
            lines.add(line.strip());
        }
        return String.join("\n", lines).strip();
    }

    private static String seeItem(Block block) {
        String line = block.lines.isEmpty() ? "" : block.lines.getFirst().trim();
        if (line.isBlank()) {
            return "";
        }
        if (!line.contains(" ") && !line.contains("<") && !line.contains("{")) {
            return "- `" + line + "`";
        }
        return "- " + inline(line);
    }

    static String inline(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        List<String> fragments = new ArrayList<>();
        String work = text;
        work = protect(work, CODE, fragment -> "`" + fragment + "`", fragments);
        work = protect(work, LINK, JavadocFormatter::linkText, fragments);
        work = protect(work, CODE_ELEMENT, fragment -> "`" + fragment + "`", fragments);
        work = protect(work, PRE, fragment -> "\n```java\n" + fragment.strip() + "\n```\n", fragments);
        work = ANCHOR.matcher(work).replaceAll("[$2]($1)");
        work = work.replaceAll("(?i)<br\\s*/?>", "\n");
        work = work.replaceAll("(?i)</?p[^>]*>", "\n\n");
        work = work.replaceAll("(?i)<(?:b|strong)>", "**").replaceAll("(?i)</(?:b|strong)>", "**");
        work = work.replaceAll("(?i)<(?:i|em)>", "*").replaceAll("(?i)</(?:i|em)>", "*");
        work = work.replaceAll("(?i)<li[^>]*>", "\n- ");
        work = work.replaceAll("(?i)</?(?:ul|ol|li|dl|dt|dd|blockquote|hr|div|span|tr|table|td|th|thead|tbody|h[1-6])[^>]*>", "");
        work = TAG.matcher(work).replaceAll("");
        work = restore(work, fragments);
        return work.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String linkText(String inner) {
        String value = inner.strip();
        int space = value.indexOf(' ');
        String target = space < 0 ? value : value.substring(0, space);
        String label = space < 0 ? value : value.substring(space + 1).strip();
        String shown = label.isBlank() ? target : label;
        return "`" + shown + "`";
    }

    private static String protect(String work, Pattern pattern, java.util.function.Function<String, String> mapper,
                                  List<String> fragments) {
        Matcher matcher = pattern.matcher(work);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = mapper.apply(matcher.group(1));
            int index = fragments.size();
            fragments.add(replacement);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("\uE000" + index + "\uE001"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String restore(String work, List<String> fragments) {
        Matcher matcher = PLACEHOLDER.matcher(work);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = index < fragments.size() ? fragments.get(index) : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static final class Block {
        private final String tag;
        private final List<String> lines = new ArrayList<>();

        private Block(String tag) {
            this.tag = tag;
        }
    }
}
