package com.eyecode.learning.catalog;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaKeywordRegistry;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.JavaLexerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JavaSyntaxLearningResolver {

    private static final List<String> CONTEXTUAL = List.of("var", "record", "sealed", "permits", "non-sealed", "yield");

    private final JavaSyntaxLearningCatalog catalog;
    private final JavaLexerService lexerService = new JavaLexerService();

    public JavaSyntaxLearningResolver() {
        this(new JavaSyntaxLearningCatalog());
    }

    public JavaSyntaxLearningResolver(JavaSyntaxLearningCatalog catalog) {
        this.catalog = catalog;
    }

    public Optional<com.eyecode.learning.model.LearningConcept> resolve(String source, int offset) {
        if (source == null || offset < 0 || offset > source.length()) return Optional.empty();
        List<Lexeme> lexemes = new ArrayList<>();
        for (Token token : lexerService.lex(DocumentSnapshot.oneShot(source)).tokens()) {
            if (token.type() != JavaTokenType.WHITESPACE
                    && token.type() != JavaTokenType.COMMENT
                    && token.type() != JavaTokenType.EOF) {
                lexemes.add(new Lexeme(token.text(), token.type() == JavaTokenType.KEYWORD,
                        token.startOffset(), token.endOffset()));
            }
        }
        int index = indexAt(lexemes, offset);
        return index < 0 ? Optional.empty() : resolve(lexemes, index);
    }

    public Optional<com.eyecode.learning.model.LearningConcept> resolve(SyntaxSnapshot snapshot, int offset) {
        if (snapshot == null) return Optional.empty();
        List<Lexeme> lexemes = snapshot.getTokens().stream()
                .filter(token -> token.type() != TokenType.WHITESPACE && token.type() != TokenType.COMMENT)
                .map(token -> new Lexeme(token.text(), token.type() == TokenType.KEYWORD,
                        token.startOffset(), token.endOffset()))
                .toList();
        int index = indexAt(lexemes, offset);
        return index < 0 ? Optional.empty() : resolve(lexemes, index);
    }

    public static boolean isContextualToken(String text) {
        return CONTEXTUAL.contains(text);
    }

    private Optional<com.eyecode.learning.model.LearningConcept> resolve(List<Lexeme> tokens, int index) {
        Lexeme token = tokens.get(index);
        if (!token.keyword() || !JavaKeywordRegistry.isKeyword(token.text())) return Optional.empty();
        if (isContextualToken(token.text()) && !isContextuallyValid(tokens, index, token.text())) {
            return Optional.empty();
        }
        return catalog.find(token.text());
    }

    private boolean isContextuallyValid(List<Lexeme> tokens, int index, String text) {
        return switch (text) {
            case "var" -> isVarDeclaration(tokens, index);
            case "record" -> isRecordDeclaration(tokens, index);
            case "sealed" -> isSealedModifier(tokens, index);
            case "permits" -> isPermitsClause(tokens, index);
            case "yield" -> isYieldStatement(tokens, index);
            case "non-sealed" -> false;
            default -> false;
        };
    }

    private boolean isVarDeclaration(List<Lexeme> tokens, int index) {
        if (index + 2 >= tokens.size() || !isIdentifier(tokens.get(index + 1))) return false;
        String initializer = tokens.get(index + 2).text();
        return (initializer.equals("=") || initializer.equals(":"))
                && (index == 0 || isDeclarationBoundary(tokens.get(index - 1).text()));
    }

    private boolean isRecordDeclaration(List<Lexeme> tokens, int index) {
        return index + 2 < tokens.size()
                && isIdentifier(tokens.get(index + 1))
                && tokens.get(index + 2).text().equals("(");
    }

    private boolean isSealedModifier(List<Lexeme> tokens, int index) {
        for (int i = index + 1; i < Math.min(tokens.size(), index + 8); i++) {
            String text = tokens.get(i).text();
            if (text.equals("class") || text.equals("interface")) return true;
            if (text.equals(";") || text.equals("=") || text.equals("(")) return false;
        }
        return false;
    }

    private boolean isPermitsClause(List<Lexeme> tokens, int index) {
        boolean foundDeclaration = false;
        for (int i = index - 1; i >= 0 && i >= index - 12; i--) {
            String text = tokens.get(i).text();
            if (text.equals("sealed")) return foundDeclaration;
            if (text.equals("class") || text.equals("interface")) foundDeclaration = true;
            if (text.equals("{") || text.equals(";") || text.equals("}")) return false;
        }
        return false;
    }

    private boolean isYieldStatement(List<Lexeme> tokens, int index) {
        boolean arrow = false;
        boolean switchKeyword = false;
        for (int i = index - 1; i >= 0 && i >= index - 24; i--) {
            String text = tokens.get(i).text();
            arrow |= text.equals("->");
            switchKeyword |= text.equals("switch");
            if (text.equals(";") && !arrow) break;
        }
        return arrow && switchKeyword;
    }

    private static boolean isIdentifier(Lexeme token) {
        return !token.keyword() && token.text().matches("[A-Za-z_$][A-Za-z0-9_$]*");
    }

    private static boolean isDeclarationBoundary(String text) {
        return text.equals("{") || text.equals(";") || text.equals("(") || text.equals(":");
    }

    private static int indexAt(List<Lexeme> tokens, int offset) {
        for (int i = 0; i < tokens.size(); i++) {
            Lexeme token = tokens.get(i);
            if (token.startOffset() <= offset && offset < token.endOffset()) return i;
        }
        return -1;
    }

    private record Lexeme(String text, boolean keyword, int startOffset, int endOffset) {
    }
}