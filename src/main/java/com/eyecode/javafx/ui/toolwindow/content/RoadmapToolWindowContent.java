package com.eyecode.javafx.ui.toolwindow.content;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

public final class RoadmapToolWindowContent extends ToolWindowPlaceholderContent {

    private static final String SAMPLE = """
            package com.eyecode.demo;

            public class Application {

                public static void main(String[] args) {
                    String text = "EyeCode";
                    System.out.println(text);
                }
            }
            """;

    public RoadmapToolWindowContent() {
        super("Roadmap");

        VBox content = new VBox(18);
        content.setStyle("""
                -fx-background-color: #1e1f22;
                -fx-padding: 16;
                """);

        Font font = Font.font("JetBrains Mono", 13);

        /*
         * A — Pure JavaFX Text
         */
        Label pureLabel = new Label("A — JavaFX Text");

        Text pureText = new Text(SAMPLE);
        pureText.setFont(font);
        pureText.setFill(Color.web("#dfe1e5"));
        pureText.setFontSmoothingType(FontSmoothingType.LCD);

        VBox pureBox = new VBox(8, pureLabel, pureText);

        /*
         * B — RichTextFX StyleClassedTextArea
         */
        Label styledLabel = new Label("B — StyleClassedTextArea");

        StyleClassedTextArea styledArea = new StyleClassedTextArea();
        styledArea.replaceText(SAMPLE);
        styledArea.setEditable(false);
        styledArea.setPrefHeight(190);
        styledArea.setStyle("""
                -fx-font-family: "JetBrains Mono";
                -fx-font-size: 13px;
                -fx-background-color: #1e1f22;
                """);

        VirtualizedScrollPane<StyleClassedTextArea> styledScroll =
                new VirtualizedScrollPane<>(styledArea);

        /*
         * C — RichTextFX CodeArea
         */
        Label codeLabel = new Label("C — CodeArea");

        CodeArea codeArea = new CodeArea();
        codeArea.replaceText(SAMPLE);
        codeArea.setEditable(false);
        codeArea.setPrefHeight(190);
        codeArea.setStyle("""
                -fx-font-family: "JetBrains Mono";
                -fx-font-size: 13px;
                -fx-background-color: #1e1f22;
                """);

        VirtualizedScrollPane<CodeArea> codeScroll =
                new VirtualizedScrollPane<>(codeArea);

        content.getChildren().addAll(
                pureBox,
                styledLabel,
                styledScroll,
                codeLabel,
                codeScroll
        );

        addSection("Text Rendering Comparison", content);
    }
}