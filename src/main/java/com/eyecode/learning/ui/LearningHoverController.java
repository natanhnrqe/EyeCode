package com.eyecode.learning.ui;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.learning.analysis.DefaultLearningContextResolver;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.analysis.LearningContextResolver;
import com.eyecode.learning.hover.HoverEngine;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import javax.swing.*;
import java.awt.Point;
import java.awt.event.*;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class LearningHoverController {

    private static final int HOVER_DELAY_MS = 500;
    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "record");

    private final JTextPane textPane;
    private final HoverEngine hoverEngine;
    private final Supplier<SyntaxSnapshot> syntaxSupplier;
    private final LearningContextResolver resolver;
    private final LearningHoverPopup popup;
    private final Timer hoverTimer;

    private volatile int lastOffset = -1;
    private volatile String lastSymbolKey;
    private volatile boolean popupSuppressed;

    public LearningHoverController(JTextPane textPane, HoverEngine hoverEngine, Supplier<SyntaxSnapshot> syntaxSupplier) {
        this.textPane = textPane;
        this.hoverEngine = hoverEngine;
        this.syntaxSupplier = syntaxSupplier;
        this.resolver = new DefaultLearningContextResolver();
        this.popup = new LearningHoverPopup();

        this.hoverTimer = new Timer(HOVER_DELAY_MS, e -> doShow());
        this.hoverTimer.setRepeats(false);

        installListeners();
    }

    public void dispose() {
        hoverTimer.stop();
        popup.hide();
        textPane.removeMouseMotionListener(motionListener);
        textPane.removeMouseListener(exitListener);
        textPane.removeKeyListener(keyListener);
        textPane.removeFocusListener(focusListener);
    }

    private void installListeners() {
        textPane.addMouseMotionListener(motionListener);
        textPane.addMouseListener(exitListener);
        textPane.addKeyListener(keyListener);
        textPane.addFocusListener(focusListener);
    }

    private final MouseMotionAdapter motionListener = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            if (popupSuppressed) return;

            int offset = textPane.viewToModel2D(e.getPoint());
            if (offset == lastOffset) return;

            lastOffset = offset;
            lastSymbolKey = null;
            popup.hide();
            hoverTimer.restart();
        }
    };

    private final MouseAdapter exitListener = new MouseAdapter() {
        @Override
        public void mouseExited(MouseEvent e) {
            popupSuppressed = false;
            hoverTimer.stop();
            popup.hide();
            lastOffset = -1;
            lastSymbolKey = null;
        }
    };

    private final KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            popupSuppressed = false;
            hoverTimer.stop();
            popup.hide();
            lastOffset = -1;
            lastSymbolKey = null;
        }
    };

    private final FocusAdapter focusListener = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            popupSuppressed = false;
            hoverTimer.stop();
            popup.hide();
            lastOffset = -1;
            lastSymbolKey = null;
        }
    };

    private void doShow() {
        if (popup.isVisible()) return;

        SyntaxSnapshot syntax = syntaxSupplier.get();
        if (syntax == null || syntax.isEmpty()) return;

        Optional<SyntaxToken> token = syntax.getTokens().stream()
                .filter(t -> lastOffset >= t.startOffset() && lastOffset <= t.endOffset()
                        && t.type() == TokenType.KEYWORD
                        && TYPE_KEYWORDS.contains(t.text()))
                .findFirst();

        if (token.isEmpty()) return;

        SyntaxToken t = token.get();
        String key = t.text() + ":" + t.startOffset() + ":" + t.endOffset();
        if (Objects.equals(key, lastSymbolKey)) return;
        lastSymbolKey = key;

        SymbolKind kind = keywordToKind(t.text());
        if (kind == null) return;

        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setKind(kind);
        symbol.setName(t.text());

        LearningContext ctx = new LearningContext();
        ctx.setCurrentSymbol(symbol);
        ctx.setCursorOffset(lastOffset);

        LearningAnalysisContext analysisCtx = resolver.resolve(ctx);
        if (analysisCtx == null) return;

        Optional<LearningConcept> concept = hoverEngine.resolve(analysisCtx);
        if (concept.isEmpty()) {
            lastSymbolKey = null;
            return;
        }

        Point mouseScreen = java.awt.MouseInfo.getPointerInfo().getLocation();
        popup.show(textPane, concept.get(), mouseScreen);
    }

    private static SymbolKind keywordToKind(String text) {
        return switch (text) {
            case "class" -> SymbolKind.CLASS;
            case "interface" -> SymbolKind.INTERFACE;
            case "enum" -> SymbolKind.ENUM;
            case "record" -> SymbolKind.RECORD;
            default -> null;
        };
    }
}
