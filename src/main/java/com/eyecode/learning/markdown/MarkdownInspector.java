package com.eyecode.learning.markdown;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public final class MarkdownInspector extends JFrame {

    private final MarkdownBuilder builder;
    private final MarkdownParser parser;
    private final MarkdownRenderer renderer;

    public MarkdownInspector() {
        super("Markdown Inspector");
        builder = new MarkdownBuilder();
        parser = new MarkdownParser();
        renderer = new MarkdownRenderer();
        setupWindow();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(ColorManager.WINDOW_BG);
    }

    public void inspect(LearningPage page) {
        String markdown = builder.build(page);
        MarkdownDocument doc = parser.parse(markdown);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(ColorManager.PANEL_BG);
        tabs.setForeground(ColorManager.TEXT_PRIMARY);

        tabs.addTab("Markdown", createMarkdownTab(markdown));
        tabs.addTab("AST", createAstTab(doc));
        tabs.addTab("Preview", createPreviewTab(doc));

        getContentPane().removeAll();
        getContentPane().add(tabs, BorderLayout.CENTER);
        revalidate();
        repaint();
        setVisible(true);
    }

    private JScrollPane createMarkdownTab(String markdown) {
        JTextArea area = new JTextArea(markdown);
        area.setEditable(false);
        area.setBackground(ColorManager.EDITOR_BG);
        area.setForeground(ColorManager.EDITOR_FOREGROUND);
        area.setCaretColor(ColorManager.EDITOR_FOREGROUND);
        area.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        area.setTabSize(4);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        return scroll;
    }

    private JScrollPane createAstTab(MarkdownDocument doc) {
        String ast = formatAst(doc);
        JTextArea area = new JTextArea(ast);
        area.setEditable(false);
        area.setBackground(ColorManager.EDITOR_BG);
        area.setForeground(ColorManager.EDITOR_FOREGROUND);
        area.setCaretColor(ColorManager.EDITOR_FOREGROUND);
        area.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        area.setTabSize(4);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        return scroll;
    }

    private JScrollPane createPreviewTab(MarkdownDocument doc) {
        JTextPane textPane = new JTextPane();
        StyledDocument styledDoc = renderer.render(doc);
        textPane.setDocument(styledDoc);
        textPane.setEditable(false);
        textPane.setBackground(ColorManager.CARD_BG);
        textPane.setCaretColor(ColorManager.TEXT_SECONDARY);
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    static String formatAst(MarkdownDocument doc) {
        if (doc == null || doc.nodes() == null) {
            return "MarkdownDocument(null)";
        }
        StringBuilder sb = new StringBuilder("MarkdownDocument\n");
        var nodes = doc.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            MarkdownNode node = nodes.get(i);
            String prefix = (i < nodes.size() - 1) ? "\u251C\u2500\u2500 " : "\u2514\u2500\u2500 ";
            sb.append(prefix).append(formatNode(node, prefix)).append("\n");
        }
        return sb.toString();
    }

    private static String formatNode(MarkdownNode node, String parentPrefix) {
        String childPrefix = parentPrefix.startsWith("\u2514")
                ? "    "
                : "\u2502   ";
        return switch (node) {
            case HeadingNode h -> "HeadingNode(level=" + h.level() + ", text=\"" + h.text() + "\")";
            case ParagraphNode p -> formatParagraph(p, childPrefix);
            case BulletNode b -> "BulletNode(text=\"" + b.text() + "\", checked=" + b.checked() + ")";
            case CodeBlockNode c -> "CodeBlockNode(language=" + (c.language().isEmpty() ? "none" : c.language()) + ", code=\"" + abbreviate(c.code()) + "\")";
            case DividerNode d -> "DividerNode()";
            case CalloutNode c -> "CalloutNode(type=" + c.type() + ", text=\"" + abbreviate(c.text()) + "\")";
        };
    }

    private static String formatParagraph(ParagraphNode p, String childPrefix) {
        if (p.segments() == null || p.segments().isEmpty()) {
            return "ParagraphNode(segments=[])";
        }
        StringBuilder sb = new StringBuilder("ParagraphNode\n");
        var segments = p.segments();
        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            String segPrefix = (i < segments.size() - 1)
                    ? childPrefix + "\u251C\u2500\u2500 "
                    : childPrefix + "\u2514\u2500\u2500 ";
            sb.append(segPrefix).append(formatSegment(seg)).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private static String formatSegment(Segment seg) {
        return switch (seg.type()) {
            case TEXT -> "TEXT(\"" + seg.text() + "\")";
            case BOLD -> "BOLD(\"" + seg.text() + "\")";
            case ITALIC -> "ITALIC(\"" + seg.text() + "\")";
            case CODE -> "CODE(\"" + seg.text() + "\")";
            case LINK -> "LINK(\"" + seg.text() + "\", url=" + seg.url() + ")";
        };
    }

    private static String abbreviate(String text) {
        if (text == null) return "";
        if (text.length() <= 60) return text;
        return text.substring(0, 57) + "...";
    }

    public static void show(LearningPage page) {
        SwingUtilities.invokeLater(() -> {
            MarkdownInspector inspector = new MarkdownInspector();
            inspector.inspect(page);
        });
    }

    public static void main(String[] args) {
        show(createSamplePage());
    }

    static LearningPage createSamplePage() {
        List<LearningContentSection> sections = new ArrayList<>();
        sections.add(section("intro", "O que \u00E9 uma classe?", LearningContentType.INTRODUCTION, 1,
                "Uma classe \u00E9 como um molde ou uma receita.\n\n" +
                "Ela define como algo ser\u00E1 criado, quais informa\u00E7\u00F5es esse algo ter\u00E1 " +
                "e o que ele ser\u00E1 capaz de fazer."));
        sections.add(section("analogy", "Analogia", LearningContentType.ANALOGY, 2,
                "Imagine que voc\u00EA quer fazer bolos de chocolate.\n\n" +
                "Antes de colocar a m\u00E3o na massa, voc\u00EA precisa de uma receita. " +
                "A receita diz quais ingredientes usar."));
        sections.add(section("code", "Exemplo", LearningContentType.CODE_EXAMPLE, 4,
                "class Pessoa {\n" +
                "    String nome;\n" +
                "    int idade;\n" +
                "\n" +
                "    void apresentar() {\n" +
                "        System.out.println(\"Ol\u00E1, eu sou \" + nome);\n" +
                "    }\n" +
                "}\n\n" +
                "A palavra class diz ao Java que estamos criando uma classe."));
        sections.add(section("flow", "Como funciona internamente", LearningContentType.HOW_IT_WORKS, 5,
                "Quando voc\u00EA compila um arquivo .java, o Java transforma sua classe em um arquivo .class.\n\n" +
                "Esse arquivo .class cont\u00E9m bytecode.\n\n" +
                "- A classe \u00E9 o plano.\n" +
                "- O objeto \u00E9 a constru\u00E7\u00E3o real.\n" +
                "- A JVM \u00E9 a f\u00E1brica."));
        sections.add(section("mistakes", "Erros comuns", LearningContentType.COMMON_MISTAKES, 6,
                "1. Achar que classe e objeto s\u00E3o a mesma coisa\n\n" +
                "A classe \u00E9 o molde. O objeto \u00E9 o resultado.\n\n" +
                "2. Colocar tudo dentro do m\u00E9todo main\n\n" +
                "Muitos iniciantes escrevem o programa inteiro dentro do main."));
        sections.add(section("dica", "Curiosidade", LearningContentType.CURIOSITY, 7,
                "Toda classe em Java herda de Object automaticamente. " +
                "Isso significa que toda classe j\u00E1 vem com m\u00E9todos como toString e equals."));

        LearningPage page = new LearningPage();
        page.setId("class-page");
        page.setTitle("Classes em Java");
        page.setShortDescription("Classes s\u00E3o modelos utilizados para criar objetos em Java.");
        page.setDifficulty(DifficultyLevel.BEGINNER);
        page.setSections(sections);
        return page;
    }

    private static LearningContentSection section(String id, String title,
                                                  LearningContentType type, int order, String content) {
        LearningContentSection s = new LearningContentSection();
        s.setId(id);
        s.setTitle(title);
        s.setContent(content);
        s.setType(type);
        s.setOrder(order);
        return s;
    }
}
