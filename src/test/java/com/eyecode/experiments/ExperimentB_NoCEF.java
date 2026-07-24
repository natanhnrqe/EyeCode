package com.eyecode.experiments;

import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.catalog.LearningCatalog;
import com.eyecode.learning.concepts.DefaultLearningConceptEngine;
import com.eyecode.learning.concepts.LearningConceptEngine;
import com.eyecode.learning.concepts.providers.ClassConceptProvider;
import com.eyecode.learning.hover.ConceptHoverProvider;
import com.eyecode.learning.hover.DefaultHoverEngine;
import com.eyecode.learning.hover.HoverEngine;
import com.eyecode.learning.ui.LearningCard;
import com.eyecode.learning.ui.LearningHoverController;
import com.eyecode.learning.ui.LearningHoverPopup;
import com.eyecode.learning.ui.SwingLearningHoverScheduler;
import com.eyecode.learning.ui.SwingLearningHoverSurface;
import com.eyecode.ui.swing.SwingUIViewFactory;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExperimentB_NoCEF {

    public static void main(String[] args) {
        LearningChromiumCard.USE_CEF = false;

        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Exp B — Minimal Editor + Hover NO CEF");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            JTextPane textPane = new JTextPane();
            textPane.setText("""
                    interface MyInterface {
                        void doSomething();
                    }

                    class HelloWorld implements MyInterface {
                        public void doSomething() {
                            System.out.println("hello");
                        }
                    }
                    """);

            JScrollPane scrollPane = new JScrollPane(textPane);
            frame.add(scrollPane);

            LearningHoverPopup popup = new LearningHoverPopup(new SwingUIViewFactory());
            popup.setCard(new LearningCard());

            LearningCatalog catalog = new DefaultLearningCatalog();
            ClassConceptProvider classProvider = new ClassConceptProvider(catalog);
            LearningConceptEngine conceptEngine = new DefaultLearningConceptEngine(List.of(classProvider));
            HoverEngine hoverEngine = new DefaultHoverEngine(List.of(new ConceptHoverProvider(conceptEngine)));

            new LearningHoverController(
                    new SwingLearningHoverSurface(textPane),
                    popup,
                    new SwingLearningHoverScheduler(),
                    hoverEngine,
                    () -> snapshotFromText(textPane.getText())
            );

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("=== EXP B READY (NO CEF) ===");
            System.out.println("1. Hover over 'interface' or 'class'");
            System.out.println("2. Does the popup still flash?");
            System.out.println("3. Does scrolling work?");
        });
    }

    private static SyntaxSnapshot snapshotFromText(String text) {
        List<SyntaxToken> tokens = new ArrayList<>();
        Pattern p = Pattern.compile("\\b(class|interface|enum|record)\\b");
        Matcher m = p.matcher(text);
        while (m.find()) {
            tokens.add(new SyntaxToken(TokenType.KEYWORD, m.start(), m.end(), m.group()));
        }
        return new SyntaxSnapshot(tokens);
    }
}
