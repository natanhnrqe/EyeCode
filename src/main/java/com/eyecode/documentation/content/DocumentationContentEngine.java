package com.eyecode.documentation.content;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.Normalizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentationContentEngine {

    private static final Pattern HEADING = Pattern.compile("<h([23])>(.*?)</h\\1>", Pattern.DOTALL);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern CALLOUT_MARKER_ONLY = Pattern.compile(
            "<blockquote>\\s*<p>\\[!(INFO|WARNING|NOTE|TIP)\\]</p>([\\s\\S]*?)</blockquote>");
    private static final Pattern CALLOUT_MARKER_INLINE = Pattern.compile(
            "<blockquote>\\s*<p>\\[!(INFO|WARNING|NOTE|TIP)\\]([\\s\\S]*?)</p>([\\s\\S]*?)</blockquote>");

    private final DocumentationContentRepository repository = new DocumentationContentRepository();
    private final DocumentationFrontMatterParser frontMatterParser = new DocumentationFrontMatterParser();
    private final Parser markdownParser;
    private final HtmlRenderer markdownRenderer;
    private final ConcurrentMap<String, DocumentationPage> pageCache = new ConcurrentHashMap<>();
    private volatile List<DocumentationCatalogEntry> catalogCache;

    public DocumentationContentEngine() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                AutolinkExtension.create(),
                EmojiExtension.create(),
                TablesExtension.create()
        ));
        markdownParser = Parser.builder(options).build();
        markdownRenderer = HtmlRenderer.builder(options).build();
    }

    public List<DocumentationCatalogEntry> catalog() {
        List<DocumentationCatalogEntry> cached = catalogCache;
        if (cached != null) {
            return cached;
        }
        List<DocumentationCatalogEntry> entries = new ArrayList<>();
        for (String identifier : repository.catalogIdentifiers()) {
            entries.add(DocumentationCatalogEntry.from(repository.loadFrontMatter(identifier)));
        }
        List<DocumentationCatalogEntry> built = List.copyOf(entries);
        catalogCache = built;
        return built;
    }

    public DocumentationPage page(String identifier) {
        return pageCache.computeIfAbsent(identifier, this::loadPage);
    }

    private DocumentationPage loadPage(String identifier) {
        String raw = repository.load(identifier);
        DocumentationFrontMatterParser.Parsed parsed = frontMatterParser.parse(raw, identifier);
        if (!parsed.metadata().id().equals(identifier)) {
            throw new IllegalArgumentException(
                    "Documentation front matter id does not match its location: " + identifier);
        }
        return new DocumentationPage(parsed.metadata(), render(parsed.body()));
    }

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = markdownRenderer.render(markdownParser.parse(markdown));
        return injectHeadingAnchors(transformCallouts(html));
    }

    static String injectHeadingAnchors(String html) {
        Matcher matcher = HEADING.matcher(html);
        StringBuffer rebuilt = new StringBuffer(html.length());
        Set<String> used = new HashSet<>();
        while (matcher.find()) {
            String level = matcher.group(1);
            String inner = matcher.group(2);
            String slug = slugify(TAGS.matcher(inner).replaceAll(""));
            if (slug.isEmpty()) {
                slug = "secao";
            }
            String unique = slug;
            int suffix = 2;
            while (!used.add(unique)) {
                unique = slug + "-" + suffix++;
            }
            matcher.appendReplacement(rebuilt, Matcher.quoteReplacement(
                    "<h" + level + " id=\"" + unique + "\">" + inner + "</h" + level + ">"));
        }
        matcher.appendTail(rebuilt);
        return rebuilt.toString();
    }

    static String slugify(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String normalized = plain.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("^-+|-+$", "");
    }

    static String transformCallouts(String html) {
        String markerOnly = CALLOUT_MARKER_ONLY.matcher(html).replaceAll(match ->
                callout(match.group(1), match.group(2).trim()));
        return CALLOUT_MARKER_INLINE.matcher(markerOnly).replaceAll(match ->
                callout(match.group(1), ("<p>" + match.group(2).stripLeading() + "</p>" + match.group(3)).trim()));
    }

    private static String callout(String kind, String content) {
        String lower = kind.toLowerCase();
        String label = switch (kind) {
            case "INFO" -> "Info";
            case "WARNING" -> "Atenção";
            case "NOTE" -> "Nota";
            case "TIP" -> "Dica";
            default -> kind;
        };
        return Matcher.quoteReplacement("<div class=\"docs-callout docs-callout-" + lower
                + "\"><span class=\"docs-callout-title\">" + label + "</span>"
                + "<div class=\"docs-callout-body\">" + content + "</div></div>");
    }
}
