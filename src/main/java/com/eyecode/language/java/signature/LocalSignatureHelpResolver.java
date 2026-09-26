package com.eyecode.language.java.signature;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.doc.LocalJavadocResolver;
import com.eyecode.language.inlay.InlayHint;
import com.eyecode.language.inlay.InlayHintMode;
import com.eyecode.language.semantic.JavaMemberKind;
import com.eyecode.language.semantic.JavaResolvedMember;
import com.eyecode.language.semantic.JavaTypeMemberResolver;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.language.signature.SignatureInformation;
import com.eyecode.language.signature.SignatureParameter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LocalSignatureHelpResolver {

    private static final Set<String> CALL_KEYWORDS = Set.of(
            "if", "while", "for", "switch", "catch", "synchronized", "return", "new", "this", "super",
            "assert", "throw", "do", "else", "try", "case", "yield");

    private final JavaLexerService lexerService = new JavaLexerService();
    private final JavaTypeMemberResolver memberResolver = new JavaTypeMemberResolver();
    private final LocalJavadocResolver javadocResolver = new LocalJavadocResolver();

    public Optional<SignatureHelpResult> resolve(String source, int caretOffset) {
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
        Call call = enclosingCall(tokens, offset);
        if (call == null) {
            return Optional.empty();
        }
        List<Candidate> candidates = candidates(source, tokens, call);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int activeParameter = activeParameter(tokens, call, offset);
        List<SignatureInformation> signatures = new ArrayList<>();
        for (Candidate candidate : candidates) {
            signatures.add(toSignature(candidate, activeParameter));
        }
        return Optional.of(new SignatureHelpResult(signatures, 0, activeParameter));
    }

    public List<InlayHint> resolveInlayHints(String source, int fromOffset, int toOffset, InlayHintMode mode) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        InlayHintMode resolvedMode = mode == null ? InlayHintMode.NAME : mode;
        int from = Math.max(0, Math.min(fromOffset, source.length()));
        int to = Math.max(0, Math.min(toOffset, source.length()));
        if (from > to) {
            int swap = from;
            from = to;
            to = swap;
        }
        List<Token> tokens;
        try {
            tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        } catch (RuntimeException exception) {
            return List.of();
        }
        JavaFileModel file = null;
        try {
            file = new JavaParser(new JavaTokenStream(tokens, source)).parse();
        } catch (RuntimeException ignored) {
            file = null;
        }
        Deque<InlayFrame> stack = new ArrayDeque<>();
        List<InlayHint> hints = new ArrayList<>();
        Set<Integer> declarationParens = declarationParens(source, file);
        Token previous = null;
        Token before = null;
        int angle = 0;
        for (Token token : tokens) {
            if (token.type() == JavaTokenType.WHITESPACE || token.type() == JavaTokenType.COMMENT) {
                continue;
            }
            String text = token.text();
            if (token.type() == JavaTokenType.SEPARATOR) {
                switch (text) {
                    case "(" -> {
                        if (angle == 0 && !stack.isEmpty() && stack.peek().params != null
                                && stack.peek().expectArgument) {
                            InlayFrame outer = stack.peek();
                            outer.expectArgument = false;
                            addHint(hints, outer, token.startOffset(), from, to, resolvedMode);
                        }
                        List<InlayParam> params = null;
                        boolean qualified = before != null && before.type() == JavaTokenType.SEPARATOR
                                && ".".equals(before.text());
                        if (!declarationParens.contains(token.startOffset())
                                && previous != null && previous.type() == JavaTokenType.IDENTIFIER
                                && !CALL_KEYWORDS.contains(previous.text()) && token.startOffset() <= to) {
                            params = inlayParams(source, tokens, file, new Call(token, previous, qualified));
                        }
                        stack.push(new InlayFrame(params));
                        angle = 0;
                    }
                    case "[", "{" -> {
                        stack.push(new InlayFrame(null));
                        angle = 0;
                    }
                    case ")", "]", "}" -> {
                        angle = 0;
                        if (!stack.isEmpty()) {
                            stack.pop();
                        }
                    }
                    case ";" -> angle = 0;
                    case "," -> {
                        if (angle == 0 && !stack.isEmpty() && stack.peek().params != null) {
                            InlayFrame frame = stack.peek();
                            frame.argIndex++;
                            frame.expectArgument = true;
                        }
                    }
                    default -> {
                    }
                }
            } else {
                if (token.type() == JavaTokenType.OPERATOR) {
                    if ("<".equals(text)) {
                        angle++;
                    } else if (">>".equals(text)) {
                        angle = Math.max(0, angle - 2);
                    } else if (">>>".equals(text)) {
                        angle = Math.max(0, angle - 3);
                    } else if (">".equals(text)) {
                        angle = Math.max(0, angle - 1);
                    }
                }
                if (angle == 0 && !stack.isEmpty() && stack.peek().params != null) {
                    InlayFrame frame = stack.peek();
                    if (frame.expectArgument) {
                        frame.expectArgument = false;
                        addHint(hints, frame, token.startOffset(), from, to, resolvedMode);
                    }
                }
            }
            before = previous;
            previous = token;
        }
        hints.sort((left, right) -> Integer.compare(left.offset(), right.offset()));
        return List.copyOf(hints);
    }

    private static Set<Integer> declarationParens(String source, JavaFileModel file) {
        Set<Integer> result = new HashSet<>();
        if (file == null) {
            return result;
        }
        for (JavaClassModel type : allTypes(file)) {
            for (JavaMethodModel method : type.getMethods()) {
                collectDeclarationParen(source, method.getRange(), method.getParameters(), result);
            }
            for (JavaConstructorModel constructor : type.getConstructors()) {
                collectDeclarationParen(source, constructor.getRange(), constructor.getParameters(), result);
            }
        }
        return result;
    }

    private static void collectDeclarationParen(String source, TextRange range,
                                                List<JavaParameterModel> params, Set<Integer> result) {
        if (range == null || params.isEmpty()) {
            return;
        }
        JavaParameterModel first = params.getFirst();
        if (first.getRange() == null) {
            return;
        }
        int firstParam = first.getRange().startOffset();
        int start = Math.max(0, Math.min(range.startOffset(), source.length()));
        if (firstParam <= start || firstParam > source.length()) {
            return;
        }
        for (int index = firstParam - 1; index >= start; index--) {
            char character = source.charAt(index);
            if (character == '(') {
                result.add(index);
                return;
            }
            if (character == '{' || character == ';' || character == '=' || character == ')' || character == ',') {
                return;
            }
        }
    }

    private static void addHint(List<InlayHint> hints, InlayFrame frame, int offset, int from, int to,
                                InlayHintMode mode) {
        if (frame.params == null || frame.argIndex >= frame.params.size()) {
            return;
        }
        String label = hintLabel(frame.params.get(frame.argIndex), mode);
        if (label == null || offset < from || offset > to) {
            return;
        }
        hints.add(new InlayHint(offset, label));
    }

    private static String hintLabel(InlayParam parameter, InlayHintMode mode) {
        return switch (mode) {
            case NAME -> parameter.name().isBlank() ? null : parameter.name() + ":";
            case TYPE -> parameter.type().isBlank() ? null : parameter.type() + ":";
            case BOTH -> {
                if (parameter.name().isBlank()) {
                    yield null;
                }
                yield parameter.type().isBlank()
                        ? parameter.name() + ":"
                        : parameter.name() + ": " + parameter.type();
            }
        };
    }

    private List<InlayParam> inlayParams(String source, List<Token> tokens, JavaFileModel file, Call call) {
        List<List<InlayParam>> candidates = new ArrayList<>();
        String callee = call.callee().text();
        if (call.qualified()) {
            for (JavaResolvedMember member : memberResolver.resolveMembers(source, call.callee().endOffset())) {
                if (member.kind() == JavaMemberKind.METHOD && member.name().equals(callee)) {
                    candidates.add(memberParams(file, member));
                }
            }
        } else if (file != null) {
            Set<String> seen = new LinkedHashSet<>();
            for (JavaClassModel type : enclosingTypes(file, call.callee().startOffset())) {
                collectMethodParams(type, callee, candidates, seen);
                collectInheritedParams(file, type, callee, candidates, seen, new LinkedHashSet<>());
            }
        }
        if (candidates.isEmpty()) {
            CompletionItem item = CompletionDatabase.get(callee);
            if (item != null && item.getKind() == CompletionItemKind.METHOD) {
                String label = item.getSignature() == null || item.getSignature().isBlank()
                        ? callee + "()"
                        : item.getSignature();
                candidates.add(paramsFromLabel(label));
            }
        }
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    private static List<InlayParam> memberParams(JavaFileModel file, JavaResolvedMember member) {
        if (file != null) {
            for (JavaClassModel type : allTypes(file)) {
                if (!member.owner().equals(type.getName())) {
                    continue;
                }
                for (JavaMethodModel method : type.getMethods()) {
                    if (member.name().equals(method.getName())) {
                        return method.getParameters().stream()
                                .map(parameter -> new InlayParam(parameter.getName(), parameter.getType()))
                                .toList();
                    }
                }
            }
        }
        return paramsFromLabel(member.signature());
    }

    private static void collectMethodParams(JavaClassModel type, String callee, List<List<InlayParam>> result,
                                            Set<String> seen) {
        for (JavaMethodModel method : type.getMethods()) {
            if (!callee.equals(method.getName()) || !seen.add(labelFor(method))) {
                continue;
            }
            result.add(method.getParameters().stream()
                    .map(parameter -> new InlayParam(parameter.getName(), parameter.getType()))
                    .toList());
        }
    }

    private static void collectInheritedParams(JavaFileModel file, JavaClassModel type, String callee,
                                               List<List<InlayParam>> result, Set<String> seen, Set<String> visited) {
        String parent = simpleName(type.getSuperClass());
        if (parent.isBlank() || !visited.add(parent)) {
            return;
        }
        JavaClassModel parentType = allTypes(file).stream()
                .filter(candidate -> parent.equals(candidate.getName()))
                .findFirst()
                .orElse(null);
        if (parentType == null) {
            return;
        }
        collectMethodParams(parentType, callee, result, seen);
        collectInheritedParams(file, parentType, callee, result, seen, visited);
    }

    private static List<InlayParam> paramsFromLabel(String label) {
        List<InlayParam> result = new ArrayList<>();
        for (SignatureParameter parameter : parameters(label)) {
            String text = parameter.label();
            int space = text.lastIndexOf(' ');
            if (space < 0) {
                result.add(new InlayParam("", text));
            } else {
                result.add(new InlayParam(text.substring(space + 1), text.substring(0, space)));
            }
        }
        return result;
    }

    private Call enclosingCall(List<Token> tokens, int offset) {
        Deque<Token> opens = new ArrayDeque<>();
        for (Token token : tokens) {
            if (token.endOffset() > offset) {
                break;
            }
            if (token.type() != JavaTokenType.SEPARATOR) {
                continue;
            }
            if ("(".equals(token.text())) {
                opens.push(token);
            } else if (")".equals(token.text()) && !opens.isEmpty()) {
                opens.pop();
            }
        }
        if (opens.isEmpty()) {
            return null;
        }
        Token open = opens.peek();
        Token callee = previousCodeToken(tokens, open.startOffset());
        if (callee == null || callee.type() != JavaTokenType.IDENTIFIER || CALL_KEYWORDS.contains(callee.text())) {
            return null;
        }
        Token before = previousCodeToken(tokens, callee.startOffset());
        boolean qualified = before != null && before.type() == JavaTokenType.SEPARATOR && ".".equals(before.text());
        return new Call(open, callee, qualified);
    }

    private static Token previousCodeToken(List<Token> tokens, int boundary) {
        Token previous = null;
        for (Token token : tokens) {
            if (token.endOffset() > boundary) {
                break;
            }
            if (token.type() == JavaTokenType.WHITESPACE) {
                continue;
            }
            previous = token;
        }
        return previous;
    }

    private static int activeParameter(List<Token> tokens, Call call, int offset) {
        int depth = 0;
        int commas = 0;
        for (Token token : tokens) {
            if (token.startOffset() <= call.open.endOffset()) {
                continue;
            }
            if (token.startOffset() >= offset) {
                break;
            }
            if (token.type() != JavaTokenType.SEPARATOR) {
                continue;
            }
            String text = token.text();
            if ("(".equals(text) || "[".equals(text)) {
                depth++;
            } else if (")".equals(text) || "]".equals(text)) {
                depth = Math.max(0, depth - 1);
            } else if (",".equals(text) && depth == 0) {
                commas++;
            }
        }
        return commas;
    }

    private List<Candidate> candidates(String source, List<Token> tokens, Call call) {
        Set<Candidate> result = new LinkedHashSet<>();
        String callee = call.callee.text();
        if (call.qualified) {
            for (JavaResolvedMember member : memberResolver.resolveMembers(source, call.callee.endOffset())) {
                if (member.kind() == JavaMemberKind.METHOD && member.name().equals(callee)) {
                    result.add(new Candidate(member.signature(), javadocFor(source, call)));
                }
            }
        } else {
            result.addAll(projectMethods(source, tokens, call));
        }
        if (result.isEmpty()) {
            CompletionItem item = CompletionDatabase.get(callee);
            if (item != null && item.getKind() == CompletionItemKind.METHOD) {
                String label = item.getSignature() == null || item.getSignature().isBlank()
                        ? callee + "()"
                        : item.getSignature();
                result.add(new Candidate(label, documentation(item)));
            }
        }
        return List.copyOf(result);
    }

    private List<Candidate> projectMethods(String source, List<Token> tokens, Call call) {
        JavaFileModel file;
        try {
            file = new JavaParser(new JavaTokenStream(tokens, source)).parse();
        } catch (RuntimeException exception) {
            return List.of();
        }
        String callee = call.callee.text();
        int offset = call.callee.startOffset();
        Set<String> seen = new LinkedHashSet<>();
        List<Candidate> result = new ArrayList<>();
        for (JavaClassModel type : enclosingTypes(file, offset)) {
            collectMethods(type, callee, tokens, result, seen);
            collectInherited(file, type, callee, tokens, result, seen, new LinkedHashSet<>());
        }
        return result;
    }

    private void collectMethods(JavaClassModel type, String callee, List<Token> tokens, List<Candidate> result,
                                Set<String> seen) {
        for (JavaMethodModel method : type.getMethods()) {
            if (!callee.equals(method.getName()) || !seen.add(labelFor(method))) {
                continue;
            }
            String javadoc = LocalJavadocResolver.javadocBefore(tokens, method.getRange().startOffset()).orElse("");
            result.add(new Candidate(labelFor(method), javadoc));
        }
    }

    private void collectInherited(JavaFileModel file, JavaClassModel type, String callee, List<Token> tokens,
                                  List<Candidate> result, Set<String> seen, Set<String> visited) {
        String parent = simpleName(type.getSuperClass());
        if (parent.isBlank() || !visited.add(parent)) {
            return;
        }
        JavaClassModel parentType = allTypes(file).stream()
                .filter(candidate -> parent.equals(candidate.getName()))
                .findFirst()
                .orElse(null);
        if (parentType == null) {
            return;
        }
        collectMethods(parentType, callee, tokens, result, seen);
        collectInherited(file, parentType, callee, tokens, result, seen, visited);
    }

    private static String labelFor(JavaMethodModel method) {
        String parameters = method.getParameters().stream()
                .map(JavaParameterModel::getType)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return method.getName() + "(" + parameters + ")";
    }

    private String javadocFor(String source, Call call) {
        return javadocResolver.atCaret(source, call.callee.startOffset())
                .map(LocalJavadocResolver.JavadocAtCaret::javadoc)
                .orElse("");
    }

    private static String documentation(CompletionItem item) {
        return item.getDocumentation() == null ? "" : item.getDocumentation();
    }

    private static SignatureInformation toSignature(Candidate candidate, int activeParameter) {
        List<SignatureParameter> parameters = parameters(candidate.label());
        Integer active = parameters.isEmpty() ? null : Math.min(activeParameter, parameters.size() - 1);
        return new SignatureInformation(candidate.label(), candidate.documentation(), parameters, active);
    }

    private static List<SignatureParameter> parameters(String label) {
        int open = label.indexOf('(');
        int close = label.lastIndexOf(')');
        if (open < 0 || close <= open + 1) {
            return List.of();
        }
        String inner = label.substring(open + 1, close);
        int base = open + 1;
        List<SignatureParameter> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < inner.length(); index++) {
            char character = inner.charAt(index);
            if (character == '<' || character == '(' || character == '[') {
                depth++;
            } else if (character == '>' || character == ')' || character == ']') {
                depth = Math.max(0, depth - 1);
            } else if (character == ',' && depth == 0) {
                addParameter(result, label, base + start, base + index);
                start = index + 1;
            }
        }
        addParameter(result, label, base + start, base + inner.length());
        return result;
    }

    private static void addParameter(List<SignatureParameter> result, String label, int from, int to) {
        if (from >= to) {
            return;
        }
        int start = from;
        int end = to;
        while (start < end && Character.isWhitespace(label.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(label.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return;
        }
        result.add(new SignatureParameter(label.substring(start, end), "", start, end));
    }

    private static List<JavaClassModel> enclosingTypes(JavaFileModel file, int offset) {
        List<JavaClassModel> enclosing = new ArrayList<>();
        for (JavaClassModel type : allTypes(file)) {
            if (type.getRange().startOffset() <= offset && offset <= type.getRange().endOffset()) {
                enclosing.add(type);
            }
        }
        enclosing.sort((left, right) -> Integer.compare(
                left.getRange().endOffset() - left.getRange().startOffset(),
                right.getRange().endOffset() - right.getRange().startOffset()));
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

    private record Call(Token open, Token callee, boolean qualified) {
    }

    private record Candidate(String label, String documentation) {
    }

    private record InlayParam(String name, String type) {
    }

    private static final class InlayFrame {
        private final List<InlayParam> params;
        private int argIndex;
        private boolean expectArgument = true;

        private InlayFrame(List<InlayParam> params) {
            this.params = params;
        }
    }
}
