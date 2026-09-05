package com.eyecode.language.documentation;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;

import java.util.List;
import java.util.Set;

/** Finds a conservative declaration anchor in JDK source text. */
public final class JdkSourceDeclarationLocator {

    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "record");
    private final JavaLexerService lexerService;

    public JdkSourceDeclarationLocator() {
        this(new JavaLexerService());
    }

    JdkSourceDeclarationLocator(JavaLexerService lexerService) {
        this.lexerService = lexerService;
    }

    public int find(String source, String simpleName) {
        if (source == null || simpleName == null || simpleName.isBlank()) {
            return 0;
        }
        var tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        for (int index = 0; index < tokens.size(); index++) {
            Token keyword = tokens.get(index);
            if (keyword.type() != JavaTokenType.KEYWORD || !TYPE_KEYWORDS.contains(keyword.text())) {
                continue;
            }
            int nameIndex = index + 1;
            while (nameIndex < tokens.size()
                    && (tokens.get(nameIndex).type() == JavaTokenType.WHITESPACE
                    || tokens.get(nameIndex).type() == JavaTokenType.COMMENT)) {
                nameIndex++;
            }
            if (nameIndex >= tokens.size()) {
                continue;
            }
            Token name = tokens.get(nameIndex);
            if (name.type() == JavaTokenType.IDENTIFIER && simpleName.equals(name.text())) {
                return keyword.startOffset();
            }
        }
        return 0;
    }

    public int find(String source, JdkSourceTarget target) {
        if (target == null || source == null) {
            return 0;
        }
        if (target.memberName() != null) {
            int memberOffset = findMember(source, target.memberName(), target.memberSignature());
            if (memberOffset > 0) {
                return memberOffset;
            }
        }
        String simpleName = target.qualifiedName().substring(
                target.qualifiedName().lastIndexOf('.') + 1);
        return find(source, simpleName);
    }

    private int findMember(String source, String memberName, String signature) {
        var tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        for (int index = 0; index < tokens.size(); index++) {
            Token name = tokens.get(index);
            if (name.type() != JavaTokenType.IDENTIFIER || !memberName.equals(name.text())) {
                continue;
            }
            int next = nextSignificant(tokens, index + 1);
            if (next >= tokens.size() || !"(".equals(tokens.get(next).text())) {
                continue;
            }
            int previous = previousSignificant(tokens, index - 1);
            if (previous >= 0 && ".".equals(tokens.get(previous).text())) {
                continue;
            }
            int close = matchingParen(tokens, next);
            if (close < 0) {
                continue;
            }
            if (signature != null && !signatureMatches(tokens, next, close, signature)) {
                continue;
            }
            int after = nextSignificant(tokens, close + 1);
            if (after < tokens.size() && isDeclarationTail(tokens, after)) {
                return name.startOffset();
            }
        }
        return 0;
    }

    private static boolean signatureMatches(List<Token> tokens, int open, int close,
                                            String expectedSignature) {
        String expected = expectedSignature.trim();
        if (expected.startsWith("(") && expected.endsWith(")")) {
            expected = expected.substring(1, expected.length() - 1);
        }
        List<String> expectedTypes = expected.isBlank()
                ? List.of()
                : java.util.Arrays.stream(expected.split(","))
                .map(String::trim)
                .map(JdkSourceDeclarationLocator::normalizeType)
                .toList();
        if (close == open + 1) return expectedTypes.isEmpty();
        List<String> actualTypes = new java.util.ArrayList<>();
        int segmentStart = open + 1;
        int depth = 0;
        for (int index = open + 1; index <= close; index++) {
            String text = index == close ? "," : tokens.get(index).text();
            if ("<".equals(text) || "(".equals(text) || "[".equals(text)) depth++;
            if (">".equals(text) || ")".equals(text) || "]".equals(text)) depth--;
            if (",".equals(text) && depth == 0) {
                actualTypes.add(parameterType(tokens, segmentStart, index));
                segmentStart = index + 1;
            }
        }
        return expectedTypes.equals(actualTypes);
    }

    private static String parameterType(List<Token> tokens, int start, int end) {
        List<Token> significant = new java.util.ArrayList<>();
        for (int index = start; index < end; index++) {
            Token token = tokens.get(index);
            if (token.type() != JavaTokenType.WHITESPACE && token.type() != JavaTokenType.COMMENT) {
                significant.add(token);
            }
        }
        if (significant.isEmpty()) return "";
        int nameIndex = significant.size() - 1;
        if (significant.get(nameIndex).type() == JavaTokenType.IDENTIFIER) nameIndex--;
        StringBuilder type = new StringBuilder();
        for (int index = 0; index <= nameIndex; index++) type.append(significant.get(index).text());
        return normalizeType(type.toString());
    }

    private static String normalizeType(String type) {
        return type.replace(" ", "").replace("	", "");
    }

    private static boolean isDeclarationTail(java.util.List<Token> tokens, int index) {
        String text = tokens.get(index).text();
        if ("{".equals(text) || ";".equals(text)) {
            return true;
        }
        if (!"throws".equals(text)) {
            return false;
        }
        for (int i = index + 1; i < tokens.size(); i++) {
            String next = tokens.get(i).text();
            if ("{".equals(next) || ";".equals(next)) {
                return true;
            }
        }
        return false;
    }

    private static int matchingParen(java.util.List<Token> tokens, int open) {
        int depth = 0;
        for (int index = open; index < tokens.size(); index++) {
            String text = tokens.get(index).text();
            if ("(".equals(text)) {
                depth++;
            } else if (")".equals(text) && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int nextSignificant(java.util.List<Token> tokens, int start) {
        for (int index = start; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.type() != JavaTokenType.WHITESPACE && token.type() != JavaTokenType.COMMENT) {
                return index;
            }
        }
        return tokens.size();
    }

    private static int previousSignificant(java.util.List<Token> tokens, int start) {
        for (int index = start; index >= 0; index--) {
            Token token = tokens.get(index);
            if (token.type() != JavaTokenType.WHITESPACE && token.type() != JavaTokenType.COMMENT) {
                return index;
            }
        }
        return -1;
    }
}
