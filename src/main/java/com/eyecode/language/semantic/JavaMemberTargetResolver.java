package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.documentation.JavaJdkType;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaMemberTargetResolver {

    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "record");
    private static final Set<String> DECLARATION_BOUNDARIES = Set.of(";", "{", "}", "(", ",");
    private static final Set<String> DECLARATION_MODIFIERS = Set.of(
            "public", "private", "protected", "static", "final", "volatile",
            "transient", "abstract", "synchronized");
    private static final Pattern IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");

    private final JavaLexerService lexerService;

    public JavaMemberTargetResolver() {
        this(new JavaLexerService());
    }

    JavaMemberTargetResolver(JavaLexerService lexerService) {
        this.lexerService = lexerService;
    }

    public Optional<JavaMemberTarget> resolve(String source, int offset) {
        if (source == null || source.isEmpty()) {
            return Optional.empty();
        }
        List<Token> tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        int memberIndex = tokenIndexAt(tokens, offset);
        if (memberIndex < 0 || tokens.get(memberIndex).type() != JavaTokenType.IDENTIFIER) {
            return Optional.empty();
        }
        int dotIndex = previousSignificant(tokens, memberIndex - 1);
        if (dotIndex < 0 || !".".equals(tokens.get(dotIndex).text())) {
            return Optional.empty();
        }

        Receiver receiver = receiverBefore(tokens, dotIndex);
        if (receiver == null) {
            return Optional.empty();
        }
        Map<String, String> imports = imports(source);
        Optional<JavaJdkType> type = resolveReceiver(source, tokens, receiver, imports);
        if (type.isEmpty()) {
            return Optional.empty();
        }

        String memberName = tokens.get(memberIndex).text();
        int openParen = nextSignificant(tokens, memberIndex + 1);
        if (openParen >= tokens.size() || !"(".equals(tokens.get(openParen).text())) {
            return Optional.empty();
        }
        int closeParen = matchingParen(tokens, openParen);
        if (closeParen < 0) {
            return Optional.empty();
        }
        return Optional.of(new JavaMemberTarget(
                type.get().qualifiedName(),
                memberName,
                JavaMemberKind.METHOD,
                argumentCount(tokens, openParen, closeParen),
                List.of()));
    }

    public Optional<JavaJdkType> resolveReceiverType(String source, int offset) {
        if (source == null || source.isEmpty()) {
            return Optional.empty();
        }
        List<Token> tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        int beforeCaret = lastSignificantAtOrBefore(tokens, offset);
        if (beforeCaret < 0) {
            return Optional.empty();
        }
        int dotIndex = ".".equals(tokens.get(beforeCaret).text())
                ? beforeCaret : previousSignificant(tokens, beforeCaret - 1);
        if (dotIndex < 0 || !".".equals(tokens.get(dotIndex).text())) {
            return Optional.empty();
        }
        Receiver receiver = receiverBefore(tokens, dotIndex);
        return receiver == null ? Optional.empty() : resolveReceiver(source, tokens, receiver, imports(source));
    }

    private Optional<JavaJdkType> resolveReceiver(String source, List<Token> tokens,
                                                  Receiver receiver, Map<String, String> imports) {
        if (receiver.stringLiteral()) {
            return JavaJdkTypeCatalog.findSimple("String");
        }
        if (receiver.parts().size() > 1) {
            return JavaJdkTypeCatalog.findQualified(String.join(".", receiver.parts()));
        }

        String name = receiver.parts().getFirst();
        Optional<String> declaredType = declaredType(tokens, receiver.tokenIndex(), name);
        if (declaredType.isPresent()) {
            return resolveTypeName(source, declaredType.get(), imports,
                    !declaredType.get().contains("."));
        }
        return resolveTypeName(source, name, imports, true);
    }

    private Optional<JavaJdkType> resolveTypeName(String source, String name,
                                                  Map<String, String> imports,
                                                  boolean rejectProjectShadow) {
        String simpleName = simpleName(name);
        if (rejectProjectShadow && isProjectType(source, simpleName)) {
            return Optional.empty();
        }
        String qualified = name.contains(".") ? name : imports.getOrDefault(name, name);
        return JavaJdkTypeCatalog.findQualified(qualified)
                .or(() -> JavaJdkTypeCatalog.findSimple(simpleName));
    }

    private Optional<String> declaredType(List<Token> tokens, int receiverIndex, String name) {
        List<Integer> receiverScope = scopePathAt(tokens, receiverIndex);
        for (int index = receiverIndex - 1; index >= 0; index--) {
            Token token = tokens.get(index);
            if (token.type() != JavaTokenType.IDENTIFIER || !name.equals(token.text())) {
                continue;
            }
            int typeEnd = previousSignificant(tokens, index - 1);
            if (typeEnd < 0) {
                continue;
            }
            String typeName = typeBefore(tokens, typeEnd);
            if (typeName == null || !declarationBoundary(tokens, typeBeforeIndex(tokens, typeEnd))) {
                continue;
            }
            if (!isEnclosingScope(scopePathAt(tokens, index), receiverScope)) {
                continue;
            }
            return Optional.of(typeName);
        }
        return Optional.empty();
    }

    private static String typeBefore(List<Token> tokens, int endIndex) {
        if (endIndex < 0) {
            return null;
        }
        Token end = tokens.get(endIndex);
        if (">".equals(end.text())) {
            int open = matchingGenericOpen(tokens, endIndex);
            if (open < 0) {
                return null;
            }
            endIndex = previousSignificant(tokens, open - 1);
        }
        if (endIndex < 0 || tokens.get(endIndex).type() != JavaTokenType.IDENTIFIER) {
            return null;
        }
        int start = endIndex;
        while (start >= 2) {
            int dot = previousSignificant(tokens, start - 1);
            int part = previousSignificant(tokens, dot - 1);
            if (dot < 0 || part < 0 || !".".equals(tokens.get(dot).text())
                    || tokens.get(part).type() != JavaTokenType.IDENTIFIER) {
                break;
            }
            start = part;
        }
        return tokenText(tokens, start, endIndex);
    }

    private static int typeBeforeIndex(List<Token> tokens, int endIndex) {
        if (endIndex >= 0 && ">".equals(tokens.get(endIndex).text())) {
            int open = matchingGenericOpen(tokens, endIndex);
            endIndex = open < 0 ? -1 : previousSignificant(tokens, open - 1);
        }
        if (endIndex < 0) {
            return -1;
        }
        int start = endIndex;
        while (start >= 2) {
            int dot = previousSignificant(tokens, start - 1);
            int part = previousSignificant(tokens, dot - 1);
            if (dot < 0 || part < 0 || !".".equals(tokens.get(dot).text())
                    || tokens.get(part).type() != JavaTokenType.IDENTIFIER) {
                break;
            }
            start = part;
        }
        return start;
    }

    private static boolean declarationBoundary(List<Token> tokens, int typeStart) {
        int previous = previousSignificant(tokens, typeStart - 1);
        if (previous < 0) {
            return true;
        }
        String text = tokens.get(previous).text();
        if (DECLARATION_BOUNDARIES.contains(text)) {
            return true;
        }
        while (DECLARATION_MODIFIERS.contains(text)) {
            previous = previousSignificant(tokens, previous - 1);
            if (previous < 0) {
                return true;
            }
            text = tokens.get(previous).text();
        }
        return DECLARATION_BOUNDARIES.contains(text);
    }

    private static Receiver receiverBefore(List<Token> tokens, int dotIndex) {
        int end = previousSignificant(tokens, dotIndex - 1);
        if (end < 0) {
            return null;
        }
        Token receiver = tokens.get(end);
        if (receiver.type() == JavaTokenType.STRING) {
            return new Receiver(List.of(), end, true);
        }
        if (receiver.type() != JavaTokenType.IDENTIFIER) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        int start = end;
        while (start >= 0) {
            Token part = tokens.get(start);
            if (part.type() != JavaTokenType.IDENTIFIER) {
                break;
            }
            parts.add(0, part.text());
            int dot = previousSignificant(tokens, start - 1);
            int previous = previousSignificant(tokens, dot - 1);
            if (dot < 0 || previous < 0 || !".".equals(tokens.get(dot).text())) {
                break;
            }
            start = previous;
        }
        if (parts.contains("this") || parts.contains("super")) {
            return null;
        }
        return new Receiver(parts, start, false);
    }

    private static int argumentCount(List<Token> tokens, int open, int close) {
        if (nextSignificant(tokens, open + 1) == close) {
            return 0;
        }
        int count = 1;
        int depth = 0;
        for (int index = open + 1; index < close; index++) {
            String text = tokens.get(index).text();
            if ("(".equals(text) || "[".equals(text) || "{".equals(text)) {
                depth++;
            } else if (")".equals(text) || "]".equals(text) || "}".equals(text)) {
                depth--;
            } else if (",".equals(text) && depth == 0) {
                count++;
            }
        }
        return count;
    }

    private static int matchingParen(List<Token> tokens, int open) {
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

    private static int matchingGenericOpen(List<Token> tokens, int close) {
        int depth = 0;
        for (int index = close; index >= 0; index--) {
            String text = tokens.get(index).text();
            if (">".equals(text)) {
                depth++;
            } else if ("<".equals(text) && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int tokenIndexAt(List<Token> tokens, int offset) {
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.startOffset() <= offset && offset < token.endOffset()) {
                return index;
            }
        }
        return -1;
    }

    private static int nextSignificant(List<Token> tokens, int start) {
        for (int index = start; index < tokens.size(); index++) {
            JavaTokenType type = (JavaTokenType) tokens.get(index).type();
            if (type != JavaTokenType.WHITESPACE && type != JavaTokenType.COMMENT) {
                return index;
            }
        }
        return tokens.size();
    }

    private static int previousSignificant(List<Token> tokens, int start) {
        for (int index = start; index >= 0; index--) {
            JavaTokenType type = (JavaTokenType) tokens.get(index).type();
            if (type != JavaTokenType.WHITESPACE && type != JavaTokenType.COMMENT) {
                return index;
            }
        }
        return -1;
    }

    private static int lastSignificantAtOrBefore(List<Token> tokens, int offset) {
        int safeOffset = Math.max(0, offset);
        for (int index = tokens.size() - 1; index >= 0; index--) {
            Token token = tokens.get(index);
            if (token.endOffset() > safeOffset) {
                continue;
            }
            JavaTokenType type = (JavaTokenType) token.type();
            if (type != JavaTokenType.WHITESPACE && type != JavaTokenType.COMMENT) {
                return index;
            }
        }
        return -1;
    }

    private static List<Integer> scopePathAt(List<Token> tokens, int endIndex) {
        List<Integer> path = new ArrayList<>();
        for (int index = 0; index <= endIndex && index < tokens.size(); index++) {
            String text = tokens.get(index).text();
            if ("{".equals(text)) {
                path.add(index);
            } else if ("}".equals(text) && !path.isEmpty()) {
                path.removeLast();
            }
        }
        return path;
    }

    private static boolean isEnclosingScope(List<Integer> declaration, List<Integer> usage) {
        if (declaration.size() > usage.size()) {
            return false;
        }
        return usage.subList(0, declaration.size()).equals(declaration);
    }

    private static String tokenText(List<Token> tokens, int start, int end) {
        StringBuilder text = new StringBuilder();
        for (int index = start; index <= end; index++) {
            text.append(tokens.get(index).text());
        }
        return text.toString();
    }

    private static Map<String, String> imports(String source) {
        Map<String, String> imports = new HashMap<>();
        Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            String qualified = matcher.group(1);
            imports.put(simpleName(qualified), qualified);
        }
        return imports;
    }

    private static boolean isProjectType(String source, String simpleName) {
        var tokens = new JavaLexerService().lex(DocumentSnapshot.oneShot(source)).tokens();
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.type() != JavaTokenType.KEYWORD || !TYPE_KEYWORDS.contains(token.text())) {
                continue;
            }
            int name = nextSignificant(tokens, index + 1);
            if (name < tokens.size() && simpleName.equals(tokens.get(name).text())) {
                return true;
            }
        }
        return false;
    }

    private static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private record Receiver(List<String> parts, int tokenIndex, boolean stringLiteral) {
    }
}
