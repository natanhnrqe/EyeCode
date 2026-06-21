package com.eyecode.editor.v2.feature;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BasicSyntaxHighlightFeature implements EditorFeature {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while"
    );
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[A-Za-z_$][A-Za-z0-9_$]*\\b");

    private EditorBuffer buffer;
    private List<SyntaxToken> keywordTokens = List.of();

    @Override
    public void attach(EditorBuffer buffer) {
        this.buffer = buffer;
        rebuildTokens();
    }

    @Override
    public void detach() {
        this.buffer = null;
        this.keywordTokens = List.of();
    }

    @Override
    public void onDocumentChanged() {
        rebuildTokens();
    }

    @Override
    public void onCaretMoved(EditorPosition position) {
        // Syntax metadata is independent of caret movement for now.
    }

    public List<SyntaxToken> getKeywordTokens() {
        return List.copyOf(keywordTokens);
    }

    private void rebuildTokens() {
        if (buffer == null) return;

        List<SyntaxToken> tokens = new ArrayList<>();
        String text = buffer.getDocument().getText();
        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (JAVA_KEYWORDS.contains(word)) {
                tokens.add(new SyntaxToken(word, matcher.start(), matcher.end(), SyntaxTokenType.KEYWORD));
            }
        }
        keywordTokens = List.copyOf(tokens);
    }

    public record SyntaxToken(String text, int startOffset, int endOffset, SyntaxTokenType type) {
    }

    public enum SyntaxTokenType {
        KEYWORD
    }
}
