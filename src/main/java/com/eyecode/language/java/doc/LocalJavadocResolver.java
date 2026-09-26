package com.eyecode.language.java.doc;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LocalJavadocResolver {

    public record JavadocAtCaret(String symbol, int rangeStart, int rangeEnd, boolean declared,
                                 boolean variable, String javadoc) {
    }

    private final JavaLexerService lexerService = new JavaLexerService();

    public Optional<JavadocAtCaret> atCaret(String source, int caretOffset) {
        if (source == null || source.isEmpty()) {
            return Optional.empty();
        }
        int offset = Math.max(0, Math.min(caretOffset, source.length()));
        List<Token> tokens;
        try {
            tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        Token symbol = symbolAt(tokens, offset);
        if (symbol == null) {
            return Optional.empty();
        }
        JavaFileModel file = null;
        try {
            file = new JavaParser(new JavaTokenStream(tokens, source)).parse();
        } catch (RuntimeException ignored) {
            // no declaration model available for this source
        }
        int declarationStart = file == null ? -1 : declarationStart(file, symbol);
        boolean variable = file != null && isVariableReference(file, symbol);
        String javadoc = declarationStart >= 0 ? javadocBefore(tokens, declarationStart).orElse("") : "";
        return Optional.of(new JavadocAtCaret(symbol.text(), symbol.startOffset(), symbol.endOffset(),
                declarationStart >= 0, variable, javadoc));
    }

    public static Optional<String> javadocBefore(List<Token> tokens, int declarationStart) {
        int index = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).startOffset() >= declarationStart) {
                index = i - 1;
                break;
            }
        }
        if (index < 0 && !tokens.isEmpty() && tokens.getLast().startOffset() < declarationStart) {
            index = tokens.size() - 1;
        }
        for (int i = index; i >= 0; i--) {
            Token token = tokens.get(i);
            if (token.type() == JavaTokenType.WHITESPACE) {
                continue;
            }
            if (token.type() == JavaTokenType.COMMENT && token.text().startsWith("/**")) {
                return Optional.of(clean(token.text()));
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    static String clean(String comment) {
        String body = comment.startsWith("/**") ? comment.substring(3) : comment;
        if (body.endsWith("*/")) {
            body = body.substring(0, body.length() - 2);
        }
        StringBuilder result = new StringBuilder();
        for (String line : body.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1);
                if (trimmed.startsWith(" ")) {
                    trimmed = trimmed.substring(1);
                }
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(trimmed);
        }
        return result.toString().trim();
    }

    private static Token symbolAt(List<Token> tokens, int offset) {
        for (Token token : tokens) {
            if (token.startOffset() > offset) {
                break;
            }
            if (offset <= token.endOffset()
                    && (token.type() == JavaTokenType.IDENTIFIER || token.type() == JavaTokenType.KEYWORD)) {
                return token;
            }
        }
        return null;
    }

    private static int declarationStart(JavaFileModel file, Token symbol) {
        String name = symbol.text();
        for (JavaClassModel type : enclosingTypes(file, symbol.startOffset())) {
            int found = typeMemberStart(type, name);
            if (found >= 0) {
                return found;
            }
            int inherited = inheritedMemberStart(file, type, name, new HashSet<>());
            if (inherited >= 0) {
                return inherited;
            }
        }
        for (JavaClassModel type : allTypes(file)) {
            if (name.equals(type.getName())) {
                return type.getRange().startOffset();
            }
        }
        return -1;
    }

    private static int typeMemberStart(JavaClassModel type, String name) {
        for (JavaMethodModel method : type.getMethods()) {
            if (name.equals(method.getName())) {
                return method.getRange().startOffset();
            }
        }
        for (JavaFieldModel field : type.getFields()) {
            if (name.equals(field.getName())) {
                return field.getRange().startOffset();
            }
        }
        return -1;
    }

    private static int inheritedMemberStart(JavaFileModel file, JavaClassModel type, String name, Set<String> seen) {
        String parent = simpleName(type.getSuperClass());
        if (parent.isBlank() || !seen.add(parent)) {
            return -1;
        }
        JavaClassModel parentType = allTypes(file).stream()
                .filter(candidate -> parent.equals(candidate.getName()))
                .findFirst()
                .orElse(null);
        if (parentType == null) {
            return -1;
        }
        int found = typeMemberStart(parentType, name);
        return found >= 0 ? found : inheritedMemberStart(file, parentType, name, seen);
    }

    private static boolean isVariableReference(JavaFileModel file, Token symbol) {
        String name = symbol.text();
        int offset = symbol.startOffset();
        for (JavaClassModel type : enclosingTypes(file, offset)) {
            for (JavaMethodModel method : type.getMethods()) {
                if (offset < method.getRange().startOffset() || offset > method.getRange().endOffset()) {
                    continue;
                }
                for (JavaParameterModel parameter : method.getParameters()) {
                    if (name.equals(parameter.getName())) {
                        return true;
                    }
                }
                for (JavaVariableModel variable : method.getLocalVariables()) {
                    if (name.equals(variable.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<JavaClassModel> enclosingTypes(JavaFileModel file, int offset) {
        List<JavaClassModel> enclosing = new ArrayList<>();
        for (JavaClassModel type : allTypes(file)) {
            if (type.getRange().startOffset() <= offset && offset <= type.getRange().endOffset()) {
                enclosing.add(type);
            }
        }
        enclosing.sort(Comparator.comparingInt(type ->
                type.getRange().endOffset() - type.getRange().startOffset()));
        return enclosing;
    }

    private static List<JavaClassModel> allTypes(JavaFileModel file) {
        List<JavaClassModel> result = new ArrayList<>(file.getTypes());
        for (int index = 0; index < result.size(); index++) {
            result.addAll(result.get(index).getNestedTypes());
        }
        return result;
    }

    private static String simpleName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }
}
