package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public final class DocumentationPanel extends JPanel {

    private static final int MAX_HEIGHT = 200;
    private static final int PADDING_V = 10;
    private static final int PADDING_H = 14;

    private final JEditorPane htmlPane;

    DocumentationPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER),
                BorderFactory.createEmptyBorder(PADDING_V, PADDING_H, PADDING_V, PADDING_H)
        ));

        htmlPane = new JEditorPane() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setBackground(new Color(0, 0, 0, 0));
        htmlPane.setForeground(ColorManager.TEXT_SECONDARY);
        htmlPane.setFont(TypographyManager.UI_SMALL());
        htmlPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        htmlPane.setMargin(new Insets(0, 0, 0, 0));

        add(htmlPane, BorderLayout.CENTER);
        setVisible(false);
    }

    void update(CompletionItem item) {
        if (item == null || !hasAnyContent(item)) {
            setVisible(false);
            return;
        }

        String html = buildHtml(item);
        htmlPane.setText(html);
        htmlPane.setCaretPosition(0);

        setVisible(true);

        int contentHeight = htmlPane.getPreferredSize().height + (PADDING_V * 2);
        setPreferredSize(new Dimension(460, Math.min(contentHeight, MAX_HEIGHT)));
    }

    private boolean hasAnyContent(CompletionItem item) {
        return (item.getLabel() != null && !item.getLabel().isBlank())
                || (item.getSignature() != null && !item.getSignature().isBlank())
                || (item.getReturnType() != null && !item.getReturnType().isBlank())
                || (item.getOwner() != null && !item.getOwner().isBlank())
                || (item.getDocumentation() != null && !item.getDocumentation().isBlank())
                || (item.getExample() != null && !item.getExample().isBlank())
                || (item.getCategory() != null && !item.getCategory().isBlank())
                || (item.getDetail() != null && !item.getDetail().isBlank());
    }

    private String buildHtml(CompletionItem item) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:monospaced;font-size:11px;color:#BBB;margin:0;padding:0;'>");

        String label = item.getLabel();
        if (label != null && !label.isBlank()) {
            html.append("<b style='font-size:12px;color:#DCDCDC;'>").append(escape(label)).append("</b>");
        }

        String owner = item.getOwner();
        if (owner != null && !owner.isBlank()) {
            html.append("<br><span style='color:#7A7E85;'>").append(escape(owner)).append("</span>");
        }

        String category = item.getCategory();
        if (category != null && !category.isBlank()) {
            html.append(" <span style='color:#7897BB;'>[").append(escape(category)).append("]</span>");
        }

        html.append("<br>");

        String signature = item.getSignature();
        if (signature != null && !signature.isBlank()) {
            html.append("<br><b style='color:#CCCCCC;'>").append(escape(signature)).append("</b>");
        }

        String returnType = item.getReturnType();
        if (returnType != null && !returnType.isBlank()) {
            html.append("<br><span style='color:#7897BB;'>Returns: </span><span style='color:#BCC4D0;'>")
                    .append(escape(returnType)).append("</span>");
        }

        String doc = item.getDocumentation();
        if (doc != null && !doc.isBlank()) {
            html.append("<br><br>").append(escape(doc));
        }

        String example = item.getExample();
        if (example != null && !example.isBlank()) {
            html.append("<br><br><span style='color:#888;'>Example:</span><br>")
                    .append("<code style='color:#6A8759;'>")
                    .append(escape(example).replace("\n", "<br>"))
                    .append("</code>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;")
                .replace("\"", "&quot;");
    }
}
