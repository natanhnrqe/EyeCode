package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public final class DocumentationPanel extends JTextPane {

    private static final int H_PADDING = 14;
    private static final int V_PADDING = 10;

    public DocumentationPanel() {

        setEditorKit(new HTMLEditorKit());
        setContentType("text/html");

        setEditable(false);
        setOpaque(false);

        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER),
                BorderFactory.createEmptyBorder(
                        V_PADDING,
                        H_PADDING,
                        V_PADDING,
                        H_PADDING
                )
        ));

        setFont(TypographyManager.UI_SMALL());

        HTMLEditorKit kit = (HTMLEditorKit) getEditorKit();
        StyleSheet css = kit.getStyleSheet();

        css.addRule("""
body{
    font-family:Segoe UI;
    font-size:12px;
    color:#BBBBBB;
    margin:0;
    padding:0;
    line-height:1.45;
}
""");

        css.addRule("""
.signature{
    font-size:13px;
    font-weight:bold;
    color:#DDDDDD;
    margin-bottom:6px;
}
""");

        css.addRule("""
.return{
    color:#7FA9FF;
    margin-bottom:8px;
}
""");

        css.addRule("""
.doc{
    margin-bottom:12px;
}
""");

        css.addRule("""
.title{
    color:#A0A0A0;
    font-weight:bold;
    margin-top:8px;
    margin-bottom:4px;
}
""");

        css.addRule("""
.owner{
    color:#808080;
    font-size:11px;
    margin-top:8px;
}
""");

        css.addRule("""
pre{
    background:#2B2B2B;
    border:1px solid #3C3F41;
    padding:8px;
    color:#6A8759;
    font-family:Consolas;
    font-size:12px;
}
""");
    }

    public void update(CompletionItem item) {

        if (item == null) {
            setText("");
            return;
        }

        setText(buildHtml(item));

        setCaretPosition(0);
    }

    public int preferredHeight(int width) {

        setSize(width, Integer.MAX_VALUE);

        return getPreferredSize().height;
    }

    private String buildHtml(CompletionItem item) {

        StringBuilder html = new StringBuilder();

        html.append("<html><body>");

        if (notBlank(item.getSignature())) {

            html.append("<div class='signature'>")
                    .append(escape(item.getSignature()))
                    .append("</div>");
        }

        if (notBlank(item.getReturnType())) {

            html.append("<div class='return'>Returns: ")
                    .append(escape(item.getReturnType()))
                    .append("</div>");
        }

        if (notBlank(item.getDocumentation())) {

            html.append("<div class='doc'>")
                    .append(escape(item.getDocumentation()).replace("\n","<br>"))
                    .append("</div>");
        }

        if (notBlank(item.getExample())) {

            html.append("<div class='title'>Example</div>");

            html.append("<pre>")
                    .append(escape(item.getExample()))
                    .append("</pre>");
        }

        if (notBlank(item.getOwner())) {

            html.append("<div class='owner'>Owner<br>")
                    .append(escape(item.getOwner()))
                    .append("</div>");
        }

        html.append("</body></html>");

        return html.toString();
    }

    private boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    private String escape(String text) {

        return text
                .replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"","&quot;")
                .replace("'","&#39;");
    }
}