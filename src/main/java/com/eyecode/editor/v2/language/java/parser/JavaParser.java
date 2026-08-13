package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.language.Token;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaModifier;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class JavaParser {

    private final JavaTokenStream stream;
    private JavaMethodModel currentMethod;
    private boolean switchLabelContext;

    public JavaParser(JavaTokenStream stream) {
        this.stream = stream;
    }

    public JavaFileModel parse() {
        JavaFileModel model = new JavaFileModel();
        List<AstNode> cuChildren = new ArrayList<>();

        int cuStart = stream.peek().startOffset();
        skipTrivia();
        parsePackage(model, cuChildren);
        skipTrivia();
        parseImports(model, cuChildren);
        skipTrivia();
        parseTypes(model, cuChildren);
        int cuEnd = stream.previous().endOffset();

        TextRange cuRange = TextRange.of(cuStart, cuEnd);
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, cuRange, cuChildren);
        AstNodes.linkParents(root);
        model.setAstRoot(root);
        model.setRange(cuRange);
        return model;
    }

    private void parsePackage(JavaFileModel model, List<AstNode> cuChildren) {
        skipTrivia();
        if (!stream.match(JavaTokenType.KEYWORD, "package")) {
            return;
        }
        int start = stream.previous().startOffset();

        skipTrivia();
        StringBuilder sb = new StringBuilder();
        sb.append(stream.expect(JavaTokenType.IDENTIFIER).text());

        while (true) {
            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ".")) break;
            sb.append(".");
            skipTrivia();
            sb.append(stream.expect(JavaTokenType.IDENTIFIER).text());
        }

        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");
        int end = stream.previous().endOffset();
        model.setPackageName(sb.toString());
        model.setRange(TextRange.of(start, end));
        cuChildren.add(AstNode.of(AstNodeKind.PACKAGE_DECLARATION, TextRange.of(start, end), List.of()));
    }

    private void parseImports(JavaFileModel model, List<AstNode> cuChildren) {
        skipTrivia();
        while (stream.peek().type() == JavaTokenType.KEYWORD
                && stream.peek().text().equals("import")) {
            int start = stream.peek().startOffset();
            stream.consume();
            skipTrivia();

            StringBuilder sb = new StringBuilder();
            if (stream.match(JavaTokenType.KEYWORD, "static")) {
                sb.append("static ");
                skipTrivia();
            }

            sb.append(stream.expect(JavaTokenType.IDENTIFIER).text());

            while (true) {
                skipTrivia();
                if (!stream.match(JavaTokenType.SEPARATOR, ".")) break;
                sb.append(".");
                skipTrivia();
                Token next = stream.peek();
                if (next.type() == JavaTokenType.OPERATOR && next.text().equals("*")) {
                    sb.append("*");
                    stream.consume();
                    break;
                }
                sb.append(stream.expect(JavaTokenType.IDENTIFIER).text());
            }

            skipTrivia();
            stream.expect(JavaTokenType.SEPARATOR, ";");
            int end = stream.previous().endOffset();
            model.getImports().add(sb.toString());
            cuChildren.add(AstNode.of(AstNodeKind.IMPORT_DECLARATION, TextRange.of(start, end), List.of()));
            skipTrivia();
        }
    }

    private void parseTypes(JavaFileModel model, List<AstNode> cuChildren) {
        while (!stream.isEOF()) {
            parseType(model, cuChildren);
        }
    }

    private void parseType(JavaFileModel model, List<AstNode> cuChildren) {
        skipTrivia();
        if (stream.isEOF()) return;

        List<TextRange> annotations = consumeAnnotations();
        List<TextRange> modifierRanges = new ArrayList<>();
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            Token modifierToken = stream.consume();
            modifierRanges.add(modifierToken.range());
            JavaModifier mod = toModifier(modifierToken.text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        Token typeToken = stream.peek();
        TypeKind kind = detectTypeKind(typeToken);

        if (kind == null) {
            stream.consume();
            return;
        }

        int declStart = !annotations.isEmpty()
                ? annotations.get(0).startOffset()
                : (!modifierRanges.isEmpty()
                        ? modifierRanges.get(0).startOffset()
                        : typeToken.startOffset());
        stream.consume();
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);

        JavaClassModel classModel = new JavaClassModel();
        classModel.setName(nameToken.text());
        classModel.setKind(kind);
        classModel.setModifiers(modifiers);

        parseTypeHeader(classModel);

        skipToBodyOrSemicolon();

        List<AstNode> memberNodes = new ArrayList<>();
        if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals("{")) {
            memberNodes = parseClassBody(classModel);
        } else if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals(";")) {
            stream.consume();
        }
        int declEnd = stream.previous().endOffset();

        TextRange declRange = TextRange.of(declStart, declEnd);
        classModel.setRange(declRange);

        List<AstNode> children = new ArrayList<>(modifierRanges.size() + annotations.size() + memberNodes.size());
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.addAll(memberNodes);

        cuChildren.add(AstNode.of(kindNode(kind), declRange, children));
        model.getTypes().add(classModel);
    }

    private static AstNodeKind kindNode(TypeKind kind) {
        return switch (kind) {
            case CLASS -> AstNodeKind.CLASS_DECLARATION;
            case INTERFACE -> AstNodeKind.INTERFACE_DECLARATION;
            case ENUM -> AstNodeKind.ENUM_DECLARATION;
            case RECORD -> AstNodeKind.RECORD_DECLARATION;
        };
    }

    private void parseTypeHeader(JavaClassModel classModel) {
        skipTrivia();
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            Token current = stream.peek();

            if (current.type() == JavaTokenType.KEYWORD && current.text().equals("extends")) {
                stream.consume();
                skipTrivia();
                classModel.setSuperClass(parseTypeName());
                skipTrivia();
                continue;
            }

            if (current.type() == JavaTokenType.KEYWORD && current.text().equals("implements")) {
                stream.consume();
                skipTrivia();
                parseInterfaceList(classModel);
                skipTrivia();
                continue;
            }

            stream.consume();
            skipTrivia();
        }
    }

    private void parseInterfaceList(JavaClassModel classModel) {
        skipTrivia();
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            classModel.getInterfaces().add(parseTypeName());
            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
            skipTrivia();
        }
    }

    private String parseTypeName() {
        StringBuilder sb = new StringBuilder();

        Token first = stream.peek();
        if (first.type() != JavaTokenType.IDENTIFIER && first.type() != JavaTokenType.KEYWORD) {
            return "";
        }
        sb.append(stream.consume().text());

        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            Token next = stream.peek();
            if (next.type() != JavaTokenType.IDENTIFIER && next.type() != JavaTokenType.KEYWORD) {
                break;
            }
            sb.append(stream.consume().text());
        }

        return sb.toString();
    }

    private boolean isBodyOrSemicolon(Token token) {
        return token.type() == JavaTokenType.SEPARATOR
                && (token.text().equals("{") || token.text().equals(";"));
    }

    private List<AstNode> parseClassBody(JavaClassModel model) {
        List<AstNode> members = new ArrayList<>();
        stream.expect(JavaTokenType.SEPARATOR, "{");

        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                break;
            }

            List<TextRange> annotations = consumeAnnotations();
            int declStart = !annotations.isEmpty()
                    ? annotations.get(0).startOffset()
                    : stream.peek().startOffset();

            if (isNestedType()) {
                parseNestedType(model, members, annotations, declStart);
                continue;
            }

            if (isConstructor(model.getName())) {
                parseConstructor(model, members, annotations, declStart);
                continue;
            }

            if (isMethod()) {
                parseMethod(model, members, annotations, declStart);
                continue;
            }

            if (isField()) {
                parseField(model, members, annotations, declStart);
                continue;
            }

            members.add(skipMember());
        }
        return members;
    }

    /**
     * Consumes one or more annotation prefixes ({@code @Name[(args)]}) at the
     * current position, returning the absolute range of each.
     */
    private List<TextRange> consumeAnnotations() {
        List<TextRange> annotations = new ArrayList<>();
        while (true) {
            skipTrivia();
            Token at = stream.peek();
            if (at.type() != JavaTokenType.AT) {
                break;
            }
            stream.consume();
            skipTrivia();
            stream.expect(JavaTokenType.IDENTIFIER);
            while (stream.match(JavaTokenType.SEPARATOR, ".")) {
                skipTrivia();
                stream.expect(JavaTokenType.IDENTIFIER);
            }
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "(")) {
                int depth = 1;
                while (stream.hasNext() && depth > 0) {
                    Token token = stream.consume();
                    if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("(")) {
                        depth++;
                    } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(")")) {
                        depth--;
                    }
                }
            }
            annotations.add(TextRange.of(at.startOffset(), stream.previous().endOffset()));
        }
        return annotations;
    }

    private boolean isConstructor(String className) {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        boolean result = stream.peek().type() == JavaTokenType.IDENTIFIER
                && stream.peek().text().equals(className);
        if (result) {
            stream.consume();
            skipTrivia();
            result = stream.peek().type() == JavaTokenType.SEPARATOR
                    && stream.peek().text().equals("(");
        }

        stream.reset(mark);
        return result;
    }

    private void parseConstructor(JavaClassModel owner, List<AstNode> members,
                                  List<TextRange> annotations, int declStart) {
        List<TextRange> modifierRanges = new ArrayList<>();
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            Token modifierToken = stream.consume();
            modifierRanges.add(modifierToken.range());
            JavaModifier mod = toModifier(modifierToken.text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER, owner.getName());
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<AstNode> paramNodes = new ArrayList<>();
        List<JavaParameterModel> parameters = parseParameters(paramNodes);
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");

        AstNode bodyNode = parseBlock();
        int declEnd = stream.previous().endOffset();

        JavaConstructorModel constructor = new JavaConstructorModel();
        constructor.setName(nameToken.text());
        constructor.setModifiers(modifiers);
        constructor.setParameters(parameters);
        constructor.setOwner(owner.getName());
        constructor.setRange(TextRange.of(declStart, declEnd));
        owner.getConstructors().add(constructor);

        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + paramNodes.size() + 1);
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.addAll(paramNodes);
        children.add(bodyNode);
        members.add(AstNode.of(AstNodeKind.CONSTRUCTOR_DECLARATION,
                TextRange.of(declStart, declEnd), children));
    }

    private boolean isMethod() {
        int mark = stream.mark();
        skipTrivia();

        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        skipTypeParameterList();

        if (modifiers.contains(JavaModifier.DEFAULT) || !consumeType(true)) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();

        boolean result = stream.peek().type() == JavaTokenType.IDENTIFIER;
        if (result) {
            stream.consume();
            skipTrivia();
            result = stream.peek().type() == JavaTokenType.SEPARATOR
                    && stream.peek().text().equals("(");
        }

        stream.reset(mark);
        return result;
    }

    /**
     * Consumes a type-parameter list ({@code <T extends Number>}) when present,
     * leaving the stream at the first token after the closing {@code >}.
     */
    private void skipTypeParameterList() {
        int mark = stream.mark();
        skipTrivia();
        if (!stream.match(JavaTokenType.OPERATOR, "<")) {
            stream.reset(mark);
            return;
        }

        int depth = 1;
        while (stream.hasNext() && depth > 0) {
            Token token = stream.consume();
            if (token.type() == JavaTokenType.OPERATOR && token.text().equals("<")) {
                depth++;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">")) {
                depth--;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">>")) {
                depth -= 2;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">>>")) {
                depth -= 3;
            }
        }
        skipTrivia();
    }

    private void parseMethod(JavaClassModel owner, List<AstNode> members,
                             List<TextRange> annotations, int declStart) {
        List<TextRange> modifierRanges = new ArrayList<>();
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            Token modifierToken = stream.consume();
            modifierRanges.add(modifierToken.range());
            JavaModifier mod = toModifier(modifierToken.text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        skipTypeParameterList();

        int typeStart = stream.peek().startOffset();
        String returnType = parseTypeReference(true);
        int typeEnd = stream.previous().endOffset();
        TextRange typeRange = TextRange.of(typeStart, typeEnd);
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<AstNode> paramNodes = new ArrayList<>();
        List<JavaParameterModel> parameters = parseParameters(paramNodes);
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipThrowsClause();

        JavaMethodModel method = new JavaMethodModel();
        method.setName(nameToken.text());
        method.setReturnType(returnType);
        method.setModifiers(modifiers);
        method.setParameters(parameters);
        method.setOwner(owner.getName());
        currentMethod = method;

        AstNode bodyNode = parseMethodBody(method);
        int declEnd = stream.previous().endOffset();
        method.setRange(TextRange.of(declStart, declEnd));
        owner.getMethods().add(method);
        currentMethod = null;

        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + paramNodes.size()
                        + (bodyNode != null ? 1 : 0) + 1);
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.add(AstNode.of(AstNodeKind.TYPE, typeRange, List.of()));
        children.addAll(paramNodes);
        if (bodyNode != null) {
            children.add(bodyNode);
        }
        members.add(AstNode.of(AstNodeKind.METHOD_DECLARATION,
                TextRange.of(declStart, declEnd), children));
    }

    private boolean isField() {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        if (!consumeType(false)) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();

        boolean result = stream.peek().type() == JavaTokenType.IDENTIFIER;
        if (result) {
            stream.consume();
            skipTrivia();
            result = !stream.match(JavaTokenType.SEPARATOR, "(");
        }

        stream.reset(mark);
        return result;
    }

    private void parseField(JavaClassModel owner, List<AstNode> members,
                            List<TextRange> annotations, int declStart) {
        List<TextRange> modifierRanges = new ArrayList<>();
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            Token modifierToken = stream.consume();
            modifierRanges.add(modifierToken.range());
            JavaModifier mod = toModifier(modifierToken.text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        int typeStart = stream.peek().startOffset();
        String type = parseTypeReference(false);
        int typeEnd = stream.previous().endOffset();
        TextRange typeRange = TextRange.of(typeStart, typeEnd);
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }

        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");
        int declEnd = stream.previous().endOffset();

        JavaFieldModel field = new JavaFieldModel();
        field.setName(nameToken.text());
        field.setType(type);
        field.setModifiers(modifiers);
        field.setOwner(owner.getName());
        field.setRange(TextRange.of(declStart, declEnd));
        owner.getFields().add(field);

        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + 1);
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.add(AstNode.of(AstNodeKind.TYPE, typeRange, List.of()));
        members.add(AstNode.of(AstNodeKind.FIELD_DECLARATION,
                TextRange.of(declStart, declEnd), children));
    }

    private void parseNestedType(JavaClassModel owner, List<AstNode> members,
                                 List<TextRange> annotations, int declStart) {
        List<TextRange> modifierRanges = new ArrayList<>();
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            Token modifierToken = stream.consume();
            modifierRanges.add(modifierToken.range());
            JavaModifier mod = toModifier(modifierToken.text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        Token typeToken = stream.peek();
        TypeKind kind = detectTypeKind(typeToken);
        if (kind == null) {
            skipMember();
            return;
        }

        stream.consume();
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);

        JavaClassModel nestedType = new JavaClassModel();
        nestedType.setName(nameToken.text());
        nestedType.setKind(kind);
        nestedType.setModifiers(modifiers);
        owner.getNestedTypes().add(nestedType);

        // Parse the nested type body recursively (header + class body)
        // instead of skipping it wholesale. This ensures nested-type members
        // (fields/methods/nested-types) populate the model and AST.
        parseTypeHeader(nestedType);
        skipToBodyOrSemicolon();

        List<AstNode> memberNodes = new ArrayList<>();
        if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals("{")) {
            memberNodes = parseClassBody(nestedType);
        } else if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals(";")) {
            stream.consume();
        }
        int declEnd = stream.previous().endOffset();
        nestedType.setRange(TextRange.of(declStart, declEnd));

        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + memberNodes.size());
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.addAll(memberNodes);
        members.add(AstNode.of(kindNode(kind), TextRange.of(declStart, declEnd), children));
    }

    private AstNode skipMember() {
        int start = stream.peek().startOffset();
        int depth = 0;

        while (stream.hasNext()) {
            Token token = stream.peek();

            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(";") && depth == 0) {
                stream.consume();
                break;
            }

            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}") && depth == 0) {
                break;
            }

            token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("{")) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}")) {
                depth--;
                if (depth <= 0) {
                    break;
                }
            }
        }
        return AstNode.of(AstNodeKind.SKIPPED,
                TextRange.of(start, stream.previous().endOffset()), List.of());
    }

    private void skipUntilSemicolon() {
        int depth = 0;

        while (stream.hasNext()) {
            Token token = stream.peek();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(";") && depth == 0) {
                return;
            }

            token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals("(") || token.text().equals("[") || token.text().equals("{"))) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals(")") || token.text().equals("]") || token.text().equals("}"))) {
                depth--;
            }
        }
    }

    private AstNode parseMethodBody(JavaMethodModel method) {
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ";")) {
            return null;
        }
        return parseBlock();
    }

    private AstNode parseBlock() {
        skipTrivia();
        Token open = stream.expect(JavaTokenType.SEPARATOR, "{");
        List<AstNode> statements = new ArrayList<>();
        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                break;
            }
            statements.add(parseStatement());
        }
        return AstNode.of(AstNodeKind.BLOCK,
                TextRange.of(open.startOffset(), stream.previous().endOffset()), statements);
    }

    private AstNode parseStatement() {
        int mark = stream.mark();
        try {
            return parseStatementInternal();
        } catch (ParserException e) {
            stream.reset(mark);
            return skipUnknownStatement();
        }
    }

    private AstNode parseStatementInternal() {
        skipTrivia();
        Token token = stream.peek();

        if (token.type() == JavaTokenType.SEPARATOR) {
            if (token.text().equals(";")) {
                return AstNode.of(AstNodeKind.EMPTY_STATEMENT, stream.consume().range(), List.of());
            }
            if (token.text().equals("{")) {
                return parseBlock();
            }
        }

        if (token.type() == JavaTokenType.KEYWORD) {
            switch (token.text()) {
                case "if" -> {
                    return parseIfStatement();
                }
                case "for" -> {
                    return parseForStatement();
                }
                case "while" -> {
                    return parseWhileStatement();
                }
                case "do" -> {
                    return parseDoWhileStatement();
                }
                case "return" -> {
                    return parseReturnStatement();
                }
                case "break" -> {
                    return parseBreakStatement();
                }
                case "continue" -> {
                    return parseContinueStatement();
                }
                case "throw" -> {
                    return parseThrowStatement();
                }
                case "try" -> {
                    return parseTryStatement();
                }
                case "switch" -> {
                    return parseSwitchStatement();
                }
                case "synchronized" -> {
                    return parseSynchronizedStatement();
                }
                case "yield" -> {
                    return parseYieldStatement();
                }
                case "assert" -> {
                    return parseAssertStatement();
                }
                case "new", "this", "super" -> {
                }
                default -> {
                    if (!isTypeToken(token, false)) {
                        return skipUnknownStatement();
                    }
                }
            }
        }

        if (token.type() == JavaTokenType.IDENTIFIER && isLabeledStatement()) {
            return parseLabeledStatement();
        }

        if (isLocalVariableDeclaration()) {
            return parseLocalVariableDeclarationStatement();
        }

        return parseExpressionStatement();
    }

    private AstNode skipUnknownStatement() {
        int start = stream.peek().startOffset();
        int depth = 0;

        while (stream.hasNext()) {
            Token token = stream.peek();
            if (depth == 0 && token.type() == JavaTokenType.SEPARATOR
                    && token.text().equals(";")) {
                stream.consume();
                break;
            }
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}")) {
                if (depth <= 1) {
                    break;
                }
                depth--;
                stream.consume();
                continue;
            }

            token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals("(") || token.text().equals("[") || token.text().equals("{"))) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals(")") || token.text().equals("]") || token.text().equals("}"))) {
                depth--;
            }
        }

        return AstNode.of(AstNodeKind.SKIPPED,
                TextRange.of(start, stream.previous().endOffset()), List.of());
    }

    private boolean isLabeledStatement() {
        int mark = stream.mark();
        skipTrivia();
        boolean result = stream.peek().type() == JavaTokenType.IDENTIFIER;
        if (result) {
            stream.consume();
            skipTrivia();
            result = stream.peek().type() == JavaTokenType.OPERATOR
                    && stream.peek().text().equals(":");
        }
        stream.reset(mark);
        return result;
    }

    private AstNode parseLabeledStatement() {
        Token label = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();
        stream.expect(JavaTokenType.OPERATOR, ":");
        skipTrivia();
        AstNode statement = parseStatement();
        return AstNode.of(AstNodeKind.LABELED_STATEMENT,
                TextRange.of(label.startOffset(), statement.range().endOffset()),
                List.of(nameExpression(label), statement));
    }

    private static AstNode nameExpression(Token token) {
        return AstNode.of(AstNodeKind.NAME_EXPRESSION, token.range(), List.of(), token);
    }

    private static AstNode operatorNode(Token token) {
        return AstNode.of(AstNodeKind.OPERATOR, token.range(), List.of(), token);
    }

    private boolean isLocalVariableDeclaration() {
        int mark = stream.mark();
        skipTrivia();
        boolean result = false;
        if (consumeType(false)) {
            skipTrivia();
            result = stream.peek().type() == JavaTokenType.IDENTIFIER;
            if (result) {
                stream.consume();
                skipTrivia();
                Token next = stream.peek();
                result = (next.type() == JavaTokenType.OPERATOR && next.text().equals("="))
                        || (next.type() == JavaTokenType.SEPARATOR && next.text().equals(";"))
                        || (next.type() == JavaTokenType.SEPARATOR && next.text().equals(","));
            }
        }
        stream.reset(mark);
        return result;
    }

    private AstNode parseLocalVariableDeclarationStatement() {
        int start = stream.peek().startOffset();
        AstNode declaration = parseLocalVariableDeclaration();
        skipTrivia();
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.LOCAL_VARIABLE_DECLARATION,
                TextRange.of(start, semi.endOffset()), declaration.children());
    }

    private AstNode parseLocalVariableDeclaration() {
        int typeStart = stream.peek().startOffset();
        String type = parseTypeReference(false);
        int typeEnd = stream.previous().endOffset();
        skipTrivia();

        List<AstNode> declarators = new ArrayList<>();
        while (true) {
            Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
            int declStart = nameToken.startOffset();
            skipTrivia();

            if (stream.match(JavaTokenType.OPERATOR, "=")) {
                skipTrivia();
                AstNode initializer = parseExpression();
                declarators.add(AstNode.of(AstNodeKind.DECLARATOR,
                        TextRange.of(declStart, initializer.range().endOffset()),
                        List.of(initializer)));
                skipTrivia();
            } else {
                declarators.add(AstNode.of(AstNodeKind.DECLARATOR,
                        TextRange.of(declStart, nameToken.endOffset()), List.of()));
            }
            registerLocalVariable(nameToken.text(), type,
                    TextRange.of(nameToken.startOffset(), nameToken.endOffset()));

            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
            skipTrivia();
        }

        List<AstNode> children = new ArrayList<>(declarators.size() + 1);
        children.add(AstNode.of(AstNodeKind.TYPE, TextRange.of(typeStart, typeEnd), List.of()));
        children.addAll(declarators);
        return AstNode.of(AstNodeKind.LOCAL_VARIABLE_DECLARATION,
                TextRange.of(typeStart, stream.previous().endOffset()), children);
    }

    private void registerLocalVariable(String name, String type, TextRange range) {
        if (currentMethod == null) {
            return;
        }
        JavaVariableModel variable = new JavaVariableModel();
        variable.setName(name);
        variable.setType(type);
        variable.setOwnerMethod(currentMethod.getName());
        variable.setRange(range);
        currentMethod.getLocalVariables().add(variable);
    }

    private AstNode parseExpressionStatement() {
        int start = stream.peek().startOffset();
        AstNode expression = parseExpression();
        skipTrivia();
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.EXPRESSION_STATEMENT,
                TextRange.of(start, semi.endOffset()), List.of(expression));
    }

    private AstNode parseExpression() {
        skipTrivia();
        Token first = stream.peek();
        if (first.type() == JavaTokenType.KEYWORD && first.text().equals("switch")) {
            return parseSwitchExpression();
        }
        int mark = stream.mark();
        AstNode base = parseConditional();
        skipTrivia();
        Token operator = stream.peek();
        if (isAssignmentOperator(operator) && isLvalue(base)) {
            stream.reset(mark);
            AstNode lhs = parseConditional();
            skipTrivia();
            Token op = stream.consume();
            skipTrivia();
            AstNode rhs = parseExpression();
            return AstNode.of(AstNodeKind.ASSIGNMENT_EXPRESSION,
                    TextRange.of(lhs.range().startOffset(), rhs.range().endOffset()),
                    List.of(lhs, operatorNode(op), rhs));
        }
        return base;
    }

    private static boolean isAssignmentOperator(Token token) {
        if (token.type() != JavaTokenType.OPERATOR) {
            return false;
        }
        return switch (token.text()) {
            case "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=" -> true;
            default -> false;
        };
    }

    private static boolean isLvalue(AstNode node) {
        AstNodeKind kind = node.kind();
        return kind == AstNodeKind.NAME_EXPRESSION
                || kind == AstNodeKind.FIELD_ACCESS_EXPRESSION
                || kind == AstNodeKind.ARRAY_ACCESS_EXPRESSION;
    }

    private AstNode parseConditional() {
        AstNode condition = parseBinary(0);
        skipTrivia();
        if (!(stream.peek().type() == JavaTokenType.OPERATOR && stream.peek().text().equals("?"))) {
            return condition;
        }
        stream.consume();
        skipTrivia();
        AstNode thenExpr = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.OPERATOR, ":");
        skipTrivia();
        AstNode elseExpr = parseExpression();
        return AstNode.of(AstNodeKind.TERNARY_EXPRESSION,
                TextRange.of(condition.range().startOffset(), elseExpr.range().endOffset()),
                List.of(condition, thenExpr, elseExpr));
    }

    private AstNode parseBinary(int minPrecedence) {
        AstNode left = parseUnary();
        while (true) {
            skipTrivia();
            Token operator = stream.peek();
            int precedence = binaryPrecedence(operator);
            if (precedence < minPrecedence) {
                break;
            }
            stream.consume();
            skipTrivia();
            if (operator.type() == JavaTokenType.KEYWORD && operator.text().equals("instanceof")) {
                AstNode typeNode = parseTypeNode();
                left = AstNode.of(AstNodeKind.INSTANCEOF_EXPRESSION,
                        TextRange.of(left.range().startOffset(), typeNode.range().endOffset()),
                        List.of(left, operatorNode(operator), typeNode));
                continue;
            }
            AstNode right = parseBinary(precedence + 1);
            left = AstNode.of(AstNodeKind.BINARY_EXPRESSION,
                    TextRange.of(left.range().startOffset(), right.range().endOffset()),
                    List.of(left, operatorNode(operator), right));
        }
        return left;
    }

    private static int binaryPrecedence(Token token) {
        if (token.type() == JavaTokenType.KEYWORD && token.text().equals("instanceof")) {
            return 8;
        }
        if (token.type() != JavaTokenType.OPERATOR) {
            return -1;
        }
        return switch (token.text()) {
            case "||" -> 2;
            case "&&" -> 3;
            case "|" -> 4;
            case "^" -> 5;
            case "&" -> 6;
            case "==", "!=" -> 7;
            case "<", ">", "<=", ">=" -> 8;
            case "<<", ">>", ">>>" -> 9;
            case "+", "-" -> 10;
            case "*", "/", "%" -> 11;
            default -> -1;
        };
    }

    private AstNode parseUnary() {
        skipTrivia();
        if (isCastStart()) {
            return parseCastExpression();
        }
        Token token = stream.peek();
        if (token.type() == JavaTokenType.OPERATOR && isUnaryOperator(token.text())) {
            stream.consume();
            skipTrivia();
            AstNode operand = parseUnary();
            return AstNode.of(AstNodeKind.UNARY_EXPRESSION,
                    TextRange.of(token.startOffset(), operand.range().endOffset()),
                    List.of(operatorNode(token), operand));
        }
        return parsePostfix();
    }

    private static boolean isUnaryOperator(String text) {
        return text.equals("+") || text.equals("-") || text.equals("!") || text.equals("~")
                || text.equals("++") || text.equals("--");
    }

    /**
     * JLS 15.2 disambiguation for {@code (Type)} vs {@code (Expression)}:
     * a type in parens is a cast when it is a primitive type (always) or,
     * for reference types, when the token after {@code )} can start a cast
     * operand ({@code ! ~ ( new this super} literals or an identifier).
     * Tokens like {@code + - ++ -- instanceof [ ?} keep the parenthesized
     * interpretation (verified against javac's {@code analyzeParens}).
     */
    private boolean isCastStart() {
        int mark = stream.mark();
        skipTrivia();
        if (!stream.match(JavaTokenType.SEPARATOR, "(")) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();
        Token first = stream.peek();
        if (!isTypeToken(first, false)) {
            stream.reset(mark);
            return false;
        }
        boolean primitive = first.type() == JavaTokenType.KEYWORD;
        StringBuilder ignored = new StringBuilder();
        if (!appendTypeName(ignored, false)) {
            stream.reset(mark);
            return false;
        }
        parseGenericArguments(ignored);
        parseArraySuffix(ignored);
        skipTrivia();
        if (!stream.match(JavaTokenType.SEPARATOR, ")")) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();
        boolean cast = primitive || canStartCastOperand(stream.peek());
        stream.reset(mark);
        return cast;
    }

    private static boolean canStartCastOperand(Token token) {
        return switch (token.type()) {
            case JavaTokenType.IDENTIFIER, JavaTokenType.NUMBER, JavaTokenType.STRING,
                    JavaTokenType.CHARACTER, JavaTokenType.BOOLEAN_LITERAL,
                    JavaTokenType.NULL_LITERAL -> true;
            case JavaTokenType.KEYWORD -> token.text().equals("new") || token.text().equals("this")
                    || token.text().equals("super");
            case JavaTokenType.SEPARATOR -> token.text().equals("(");
            case JavaTokenType.OPERATOR -> token.text().equals("!") || token.text().equals("~");
            default -> false;
        };
    }

    private AstNode parseCastExpression() {
        Token open = stream.expect(JavaTokenType.SEPARATOR, "(");
        int typeStart = stream.peek().startOffset();
        StringBuilder ignored = new StringBuilder();
        appendTypeName(ignored, false);
        parseGenericArguments(ignored);
        parseArraySuffix(ignored);
        int typeEnd = stream.previous().endOffset();
        AstNode typeNode = AstNode.of(AstNodeKind.TYPE,
                TextRange.of(typeStart, typeEnd), List.of());
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode operand = parseUnary();
        return AstNode.of(AstNodeKind.CAST_EXPRESSION,
                TextRange.of(open.startOffset(), operand.range().endOffset()),
                List.of(typeNode, operand));
    }

    private AstNode parseTypeNode() {
        int typeStart = stream.peek().startOffset();
        StringBuilder ignored = new StringBuilder();
        if (!appendTypeName(ignored, false)) {
            throw stream.error("Expected type after 'instanceof'", stream.peek());
        }
        parseGenericArguments(ignored);
        parseArraySuffix(ignored);
        int typeEnd = stream.previous().endOffset();
        return AstNode.of(AstNodeKind.TYPE, TextRange.of(typeStart, typeEnd), List.of());
    }

    private boolean isLambdaParenStart() {
        int mark = stream.mark();
        skipTrivia();
        if (!stream.match(JavaTokenType.SEPARATOR, "(")) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ")")) {
            skipTrivia();
            boolean lambda = stream.match(JavaTokenType.OPERATOR, "->");
            stream.reset(mark);
            return lambda;
        }
        while (stream.hasNext()) {
            skipTrivia();
            if (stream.peek().type() != JavaTokenType.IDENTIFIER) {
                stream.reset(mark);
                return false;
            }
            stream.consume();
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, ")")) {
                skipTrivia();
                boolean lambda = stream.match(JavaTokenType.OPERATOR, "->");
                stream.reset(mark);
                return lambda;
            }
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                stream.reset(mark);
                return false;
            }
        }
        stream.reset(mark);
        return false;
    }

    private AstNode parseParenLambda() {
        Token open = stream.expect(JavaTokenType.SEPARATOR, "(");
        List<AstNode> children = new ArrayList<>();
        skipTrivia();
        if (!stream.match(JavaTokenType.SEPARATOR, ")")) {
            while (stream.hasNext()) {
                skipTrivia();
                Token name = stream.expect(JavaTokenType.IDENTIFIER);
                children.add(AstNode.of(AstNodeKind.PARAMETER, name.range(), List.of(), name));
                skipTrivia();
                if (stream.match(JavaTokenType.SEPARATOR, ")")) {
                    break;
                }
                stream.expect(JavaTokenType.SEPARATOR, ",");
            }
        }
        skipTrivia();
        Token arrow = stream.expect(JavaTokenType.OPERATOR, "->");
        children.add(operatorNode(arrow));
        skipTrivia();
        AstNode body = parseLambdaBody();
        children.add(body);
        return AstNode.of(AstNodeKind.LAMBDA_EXPRESSION,
                TextRange.of(open.startOffset(), body.range().endOffset()), children);
    }

    private AstNode parseSingleParamLambda(AstNode paramNode) {
        skipTrivia();
        Token arrow = stream.expect(JavaTokenType.OPERATOR, "->");
        skipTrivia();
        AstNode body = parseLambdaBody();
        return AstNode.of(AstNodeKind.LAMBDA_EXPRESSION,
                TextRange.of(paramNode.range().startOffset(), body.range().endOffset()),
                List.of(paramNode, operatorNode(arrow), body));
    }

    private AstNode parseLambdaBody() {
        skipTrivia();
        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("{")) {
            return parseBlock();
        }
        return parseExpression();
    }

    private AstNode parseMethodReferenceSuffix(AstNode target) {
        stream.expect(JavaTokenType.OPERATOR, "::");
        skipTrivia();
        skipTypeParameterList();
        Token member = stream.peek();
        if (member.type() != JavaTokenType.IDENTIFIER
                && !(member.type() == JavaTokenType.KEYWORD && member.text().equals("new"))) {
            throw stream.error("Expected method reference name", member);
        }
        stream.consume();
        return AstNode.of(AstNodeKind.METHOD_REFERENCE_EXPRESSION,
                TextRange.of(target.range().startOffset(), member.endOffset()),
                List.of(target), member);
    }

    private AstNode parsePostfix() {
        AstNode node = parsePrimary();
        while (true) {
            skipTrivia();
            Token token = stream.peek();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("(")) {
                node = parseCallSuffix(node);
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("[")) {
                node = parseArrayAccessSuffix(node);
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(".")) {
                node = parseFieldAccessSuffix(node);
            } else if (token.type() == JavaTokenType.OPERATOR
                    && (token.text().equals("++") || token.text().equals("--"))) {
                Token increment = stream.consume();
                node = AstNode.of(AstNodeKind.UNARY_EXPRESSION,
                        TextRange.of(node.range().startOffset(), increment.endOffset()),
                        List.of(node, operatorNode(increment)));
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals("->")
                    && node.kind() == AstNodeKind.NAME_EXPRESSION && !switchLabelContext) {
                node = parseSingleParamLambda(node);
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals("::")) {
                node = parseMethodReferenceSuffix(node);
            } else {
                break;
            }
        }
        return node;
    }

    private AstNode parsePrimary() {
        skipTrivia();
        Token token = stream.peek();
        switch (token.type()) {
            case JavaTokenType.IDENTIFIER -> {
                return nameExpression(stream.consume());
            }
            case JavaTokenType.NUMBER, JavaTokenType.STRING, JavaTokenType.CHARACTER,
                    JavaTokenType.BOOLEAN_LITERAL, JavaTokenType.NULL_LITERAL -> {
                Token literal = stream.consume();
                return AstNode.of(AstNodeKind.LITERAL_EXPRESSION, literal.range(), List.of(), literal);
            }
            case JavaTokenType.KEYWORD -> {
                if (token.text().equals("new")) {
                    return parseNewExpression();
                }
                if (token.text().equals("this")) {
                    Token thisToken = stream.consume();
                    return AstNode.of(AstNodeKind.THIS_EXPRESSION, thisToken.range(), List.of(), thisToken);
                }
                if (token.text().equals("super")) {
                    Token superToken = stream.consume();
                    return AstNode.of(AstNodeKind.SUPER_EXPRESSION, superToken.range(), List.of(), superToken);
                }
                if (isTypeToken(token, false)) {
                    Token typeToken = stream.consume();
                    return AstNode.of(AstNodeKind.NAME_EXPRESSION, typeToken.range(), List.of(), typeToken);
                }
            }
            case JavaTokenType.SEPARATOR -> {
                if (token.text().equals("(")) {
                    if (isLambdaParenStart()) {
                        return parseParenLambda();
                    }
                    stream.consume();
                    AstNode inner = parseExpression();
                    skipTrivia();
                    Token close = stream.expect(JavaTokenType.SEPARATOR, ")");
                    return AstNode.of(AstNodeKind.PARENTHESIZED_EXPRESSION,
                            TextRange.of(token.startOffset(), close.endOffset()),
                            List.of(inner));
                }
                if (token.text().equals("{")) {
                    int start = token.startOffset();
                    skipBalanced("{", "}");
                    return AstNode.of(AstNodeKind.LITERAL_EXPRESSION,
                            TextRange.of(start, stream.previous().endOffset()), List.of());
                }
            }
            default -> {
            }
        }
        throw stream.error("Unexpected token", token);
    }

    private AstNode parseCallSuffix(AstNode target) {
        Token open = stream.expect(JavaTokenType.SEPARATOR, "(");
        List<AstNode> children = new ArrayList<>();
        children.add(target);
        List<AstNode> arguments = parseArguments();
        children.addAll(arguments);
        return AstNode.of(AstNodeKind.METHOD_CALL_EXPRESSION,
                TextRange.of(target.range().startOffset(), stream.previous().endOffset()), children);
    }

    private List<AstNode> parseArguments() {
        List<AstNode> arguments = new ArrayList<>();
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ")")) {
            return arguments;
        }
        while (stream.hasNext()) {
            arguments.add(parseExpression());
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, ")")) {
                break;
            }
            stream.expect(JavaTokenType.SEPARATOR, ",");
            skipTrivia();
        }
        return arguments;
    }

    private AstNode parseFieldAccessSuffix(AstNode target) {
        stream.expect(JavaTokenType.SEPARATOR, ".");
        skipTrivia();
        if (stream.peek().type() == JavaTokenType.KEYWORD && stream.peek().text().equals("class")) {
            Token classToken = stream.consume();
            return AstNode.of(AstNodeKind.CLASS_LITERAL_EXPRESSION,
                    TextRange.of(target.range().startOffset(), classToken.endOffset()),
                    List.of(target));
        }
        skipTypeParameterList();
        Token member = stream.expect(JavaTokenType.IDENTIFIER);
        return AstNode.of(AstNodeKind.FIELD_ACCESS_EXPRESSION,
                TextRange.of(target.range().startOffset(), member.endOffset()),
                List.of(target, nameExpression(member)));
    }

    private AstNode parseArrayAccessSuffix(AstNode target) {
        Token open = stream.expect(JavaTokenType.SEPARATOR, "[");
        skipTrivia();
        AstNode index = parseExpression();
        skipTrivia();
        Token close = stream.expect(JavaTokenType.SEPARATOR, "]");
        return AstNode.of(AstNodeKind.ARRAY_ACCESS_EXPRESSION,
                TextRange.of(target.range().startOffset(), close.endOffset()),
                List.of(target, index));
    }

    private AstNode parseNewExpression() {
        Token newToken = stream.expect(JavaTokenType.KEYWORD, "new");
        skipTrivia();
        int typeStart = stream.peek().startOffset();
        StringBuilder ignored = new StringBuilder();
        appendTypeName(ignored, false);
        parseGenericArguments(ignored);
        int typeEnd = stream.previous().endOffset();
        AstNode typeNode = AstNode.of(AstNodeKind.TYPE,
                TextRange.of(typeStart, typeEnd), List.of());
        skipTrivia();

        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("[")) {
            return parseArrayCreationExpression(newToken, typeNode);
        }

        stream.match(JavaTokenType.SEPARATOR, "(");
        List<AstNode> children = new ArrayList<>();
        children.add(typeNode);
        List<AstNode> arguments = parseArguments();
        children.addAll(arguments);
        int end = stream.previous().endOffset();
        skipTrivia();
        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("{")) {
            skipBalanced("{", "}");
            end = stream.previous().endOffset();
        }
        return AstNode.of(AstNodeKind.OBJECT_CREATION_EXPRESSION,
                TextRange.of(newToken.startOffset(), end), children);
    }

    private AstNode parseArrayCreationExpression(Token newToken, AstNode typeNode) {
        List<AstNode> children = new ArrayList<>();
        children.add(typeNode);
        while (true) {
            skipTrivia();
            if (!(stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("["))) {
                break;
            }
            stream.consume();
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "]")) {
                continue;
            }
            AstNode dimension = parseExpression();
            children.add(dimension);
            skipTrivia();
            stream.expect(JavaTokenType.SEPARATOR, "]");
        }
        skipTrivia();
        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("{")) {
            int initStart = stream.peek().startOffset();
            skipBalanced("{", "}");
            children.add(AstNode.of(AstNodeKind.LITERAL_EXPRESSION,
                    TextRange.of(initStart, stream.previous().endOffset()), List.of()));
        }
        return AstNode.of(AstNodeKind.ARRAY_CREATION_EXPRESSION,
                TextRange.of(newToken.startOffset(), stream.previous().endOffset()), children);
    }

    private void skipBalanced(String open, String close) {
        int depth = 0;
        while (stream.hasNext()) {
            Token token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(open)) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(close)) {
                depth--;
                if (depth == 0) {
                    return;
                }
            }
        }
    }

    private AstNode parseIfStatement() {
        Token ifToken = stream.expect(JavaTokenType.KEYWORD, "if");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode condition = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode thenBranch = parseStatement();
        skipTrivia();
        List<AstNode> children = new ArrayList<>();
        children.add(AstNode.of(AstNodeKind.CONDITION, condition.range(), List.of(condition)));
        children.add(AstNode.of(AstNodeKind.THEN, thenBranch.range(), List.of(thenBranch)));
        int end = thenBranch.range().endOffset();
        if (stream.match(JavaTokenType.KEYWORD, "else")) {
            skipTrivia();
            AstNode elseBranch = parseStatement();
            end = elseBranch.range().endOffset();
            children.add(AstNode.of(AstNodeKind.ELSE, elseBranch.range(), List.of(elseBranch)));
        }
        return AstNode.of(AstNodeKind.IF_STATEMENT,
                TextRange.of(ifToken.startOffset(), end), children);
    }

    private AstNode parseWhileStatement() {
        Token whileToken = stream.expect(JavaTokenType.KEYWORD, "while");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode condition = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode body = parseStatement();
        return AstNode.of(AstNodeKind.WHILE_STATEMENT,
                TextRange.of(whileToken.startOffset(), body.range().endOffset()),
                List.of(AstNode.of(AstNodeKind.CONDITION, condition.range(), List.of(condition)),
                        AstNode.of(AstNodeKind.THEN, body.range(), List.of(body))));
    }

    private AstNode parseDoWhileStatement() {
        Token doToken = stream.expect(JavaTokenType.KEYWORD, "do");
        skipTrivia();
        AstNode body = parseStatement();
        skipTrivia();
        stream.expect(JavaTokenType.KEYWORD, "while");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode condition = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.DO_WHILE_STATEMENT,
                TextRange.of(doToken.startOffset(), semi.endOffset()),
                List.of(AstNode.of(AstNodeKind.THEN, body.range(), List.of(body)),
                        AstNode.of(AstNodeKind.CONDITION, condition.range(), List.of(condition))));
    }

    private AstNode parseForStatement() {
        Token forToken = stream.expect(JavaTokenType.KEYWORD, "for");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        if (isEnhancedFor()) {
            return parseEnhancedForStatement(forToken);
        }

        List<AstNode> children = new ArrayList<>();
        skipTrivia();
        if (!(stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(";"))) {
            AstNode init;
            if (isLocalVariableDeclaration()) {
                init = parseLocalVariableDeclaration();
            } else {
                init = parseExpression();
            }
            children.add(AstNode.of(AstNodeKind.INITIALIZER, init.range(), List.of(init)));
        }
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");
        skipTrivia();
        if (!(stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(";"))) {
            AstNode condition = parseExpression();
            children.add(AstNode.of(AstNodeKind.CONDITION, condition.range(), List.of(condition)));
        }
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");
        skipTrivia();
        if (!(stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(")"))) {
            AstNode update = parseExpression();
            children.add(AstNode.of(AstNodeKind.UPDATE, update.range(), List.of(update)));
        }
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode body = parseStatement();
        children.add(AstNode.of(AstNodeKind.THEN, body.range(), List.of(body)));
        return AstNode.of(AstNodeKind.FOR_STATEMENT,
                TextRange.of(forToken.startOffset(), body.range().endOffset()), children);
    }

    private boolean isEnhancedFor() {
        int mark = stream.mark();
        skipTrivia();
        boolean result = false;
        if (consumeType(false)) {
            skipTrivia();
            if (stream.peek().type() == JavaTokenType.IDENTIFIER) {
                stream.consume();
                skipTrivia();
                result = stream.peek().type() == JavaTokenType.OPERATOR
                        && stream.peek().text().equals(":");
            }
        }
        stream.reset(mark);
        return result;
    }

    private AstNode parseEnhancedForStatement(Token forToken) {
        int typeStart = stream.peek().startOffset();
        String type = parseTypeReference(false);
        int typeEnd = stream.previous().endOffset();
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();
        stream.expect(JavaTokenType.OPERATOR, ":");
        skipTrivia();
        AstNode iterable = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode body = parseStatement();
        registerLocalVariable(nameToken.text(), type,
                TextRange.of(nameToken.startOffset(), nameToken.endOffset()));
        return AstNode.of(AstNodeKind.ENHANCED_FOR_STATEMENT,
                TextRange.of(forToken.startOffset(), body.range().endOffset()),
                List.of(AstNode.of(AstNodeKind.VARIABLE,
                                TextRange.of(typeStart, nameToken.endOffset()),
                                List.of(AstNode.of(AstNodeKind.TYPE,
                                        TextRange.of(typeStart, typeEnd), List.of()))),
                        AstNode.of(AstNodeKind.ITERABLE, iterable.range(), List.of(iterable)),
                        AstNode.of(AstNodeKind.THEN, body.range(), List.of(body))));
    }

    private AstNode parseReturnStatement() {
        Token returnToken = stream.expect(JavaTokenType.KEYWORD, "return");
        skipTrivia();
        AstNode value = null;
        if (!(stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(";"))) {
            value = parseExpression();
            skipTrivia();
        }
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.RETURN_STATEMENT,
                TextRange.of(returnToken.startOffset(), semi.endOffset()),
                value == null ? List.of() : List.of(value));
    }

    private AstNode parseBreakStatement() {
        Token breakToken = stream.expect(JavaTokenType.KEYWORD, "break");
        skipTrivia();
        Token label = null;
        if (stream.peek().type() == JavaTokenType.IDENTIFIER) {
            label = stream.consume();
            skipTrivia();
        }
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.BREAK_STATEMENT,
                TextRange.of(breakToken.startOffset(), semi.endOffset()),
                label == null ? List.of() : List.of(nameExpression(label)));
    }

    private AstNode parseContinueStatement() {
        Token continueToken = stream.expect(JavaTokenType.KEYWORD, "continue");
        skipTrivia();
        Token label = null;
        if (stream.peek().type() == JavaTokenType.IDENTIFIER) {
            label = stream.consume();
            skipTrivia();
        }
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.CONTINUE_STATEMENT,
                TextRange.of(continueToken.startOffset(), semi.endOffset()),
                label == null ? List.of() : List.of(nameExpression(label)));
    }

    private AstNode parseThrowStatement() {
        Token throwToken = stream.expect(JavaTokenType.KEYWORD, "throw");
        skipTrivia();
        AstNode expression = parseExpression();
        skipTrivia();
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.THROW_STATEMENT,
                TextRange.of(throwToken.startOffset(), semi.endOffset()),
                List.of(expression));
    }

    private AstNode parseTryStatement() {
        Token tryToken = stream.expect(JavaTokenType.KEYWORD, "try");
        List<AstNode> children = new ArrayList<>();
        skipTrivia();
        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("(")) {
            skipBalanced("(", ")");
            skipTrivia();
        }
        children.add(parseBlock());
        skipTrivia();
        while (stream.match(JavaTokenType.KEYWORD, "catch")) {
            int catchStart = stream.previous().startOffset();
            skipTrivia();
            skipBalanced("(", ")");
            skipTrivia();
            AstNode catchBody = parseBlock();
            children.add(AstNode.of(AstNodeKind.CATCH_CLAUSE,
                    TextRange.of(catchStart, catchBody.range().endOffset()),
                    List.of(catchBody)));
            skipTrivia();
        }
        if (stream.match(JavaTokenType.KEYWORD, "finally")) {
            int finallyStart = stream.previous().startOffset();
            skipTrivia();
            AstNode finallyBody = parseBlock();
            children.add(AstNode.of(AstNodeKind.FINALLY_CLAUSE,
                    TextRange.of(finallyStart, finallyBody.range().endOffset()),
                    List.of(finallyBody)));
        }
        return AstNode.of(AstNodeKind.TRY_STATEMENT,
                TextRange.of(tryToken.startOffset(), stream.previous().endOffset()), children);
    }

    private AstNode parseSwitchStatement() {
        Token switchToken = stream.expect(JavaTokenType.KEYWORD, "switch");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode selector = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "{");
        List<AstNode> children = new ArrayList<>();
        children.add(AstNode.of(AstNodeKind.CONDITION, selector.range(), List.of(selector)));
        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                break;
            }
            if (stream.match(JavaTokenType.KEYWORD, "case")) {
                children.add(parseSwitchCase(stream.previous().startOffset(), false));
                continue;
            }
            if (stream.match(JavaTokenType.KEYWORD, "default")) {
                int defaultStart = stream.previous().startOffset();
                skipTrivia();
                stream.expect(JavaTokenType.OPERATOR, ":");
                int labelEnd = stream.previous().endOffset();
                children.add(parseSwitchCaseBody(TextRange.of(defaultStart, labelEnd),
                        AstNode.of(AstNodeKind.SWITCH_LABEL,
                                TextRange.of(defaultStart, labelEnd), List.of()),
                        false));
                continue;
            }
            children.add(parseStatement());
        }
        return AstNode.of(AstNodeKind.SWITCH_STATEMENT,
                TextRange.of(switchToken.startOffset(), stream.previous().endOffset()), children);
    }

    private AstNode parseSwitchCase(int caseStart, boolean expressionMode) {
        skipTrivia();
        List<AstNode> labels = new ArrayList<>();
        boolean previous = switchLabelContext;
        switchLabelContext = true;
        try {
            while (true) {
                labels.add(parseExpression());
                skipTrivia();
                if (stream.match(JavaTokenType.SEPARATOR, ",")) {
                    skipTrivia();
                    continue;
                }
                break;
            }
        } finally {
            switchLabelContext = previous;
        }
        Token terminator = stream.peek();
        boolean arrow;
        if (terminator.type() == JavaTokenType.OPERATOR && terminator.text().equals(":")) {
            stream.consume();
            arrow = false;
        } else if (terminator.type() == JavaTokenType.OPERATOR && terminator.text().equals("->")) {
            stream.consume();
            arrow = true;
        } else {
            throw stream.error("Expected ':' or '->' after case label", terminator);
        }
        int labelEnd = stream.previous().endOffset();
        if (arrow && expressionMode) {
            return parseSwitchArrowBody(caseStart, labelEnd, labels);
        }
        return parseSwitchCaseBody(TextRange.of(caseStart, labelEnd),
                AstNode.of(AstNodeKind.SWITCH_LABEL,
                        TextRange.of(caseStart, labelEnd), labels),
                expressionMode);
    }

    private AstNode parseSwitchArrowBody(int caseStart, int labelEnd, List<AstNode> labels) {
        skipTrivia();
        AstNode body;
        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("{")) {
            body = parseBlock();
        } else if (stream.peek().type() == JavaTokenType.KEYWORD && stream.peek().text().equals("throw")) {
            body = parseThrowStatement();
        } else {
            AstNode expr = parseExpression();
            skipTrivia();
            stream.expect(JavaTokenType.SEPARATOR, ";");
            body = AstNode.of(AstNodeKind.EXPRESSION_STATEMENT,
                    TextRange.of(expr.range().startOffset(), stream.previous().endOffset()),
                    List.of(expr));
        }
        List<AstNode> children = new ArrayList<>();
        children.add(AstNode.of(AstNodeKind.SWITCH_LABEL,
                TextRange.of(caseStart, labelEnd), labels));
        children.add(body);
        return AstNode.of(AstNodeKind.SWITCH_CASE,
                TextRange.of(caseStart, body.range().endOffset()), children);
    }

    private AstNode parseSwitchCaseBody(TextRange labelRange, AstNode label, boolean expressionMode) {
        List<AstNode> children = new ArrayList<>();
        children.add(label);
        int end = labelRange.endOffset();
        while (stream.hasNext()) {
            skipTrivia();
            Token token = stream.peek();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}")) {
                break;
            }
            if (token.type() == JavaTokenType.KEYWORD
                    && (token.text().equals("case") || token.text().equals("default"))) {
                break;
            }
            AstNode statement = parseStatement();
            children.add(statement);
            end = statement.range().endOffset();
        }
        return AstNode.of(AstNodeKind.SWITCH_CASE,
                TextRange.of(labelRange.startOffset(), end), children);
    }

    private AstNode parseSynchronizedStatement() {
        Token syncToken = stream.expect(JavaTokenType.KEYWORD, "synchronized");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode lock = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        AstNode body = parseBlock();
        return AstNode.of(AstNodeKind.SYNCHRONIZED_STATEMENT,
                TextRange.of(syncToken.startOffset(), body.range().endOffset()),
                List.of(AstNode.of(AstNodeKind.CONDITION, lock.range(), List.of(lock)),
                        AstNode.of(AstNodeKind.THEN, body.range(), List.of(body))));
    }

    private AstNode parseYieldStatement() {
        Token yieldToken = stream.expect(JavaTokenType.KEYWORD, "yield");
        skipTrivia();
        AstNode value = parseExpression();
        skipTrivia();
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        return AstNode.of(AstNodeKind.YIELD_STATEMENT,
                TextRange.of(yieldToken.startOffset(), semi.endOffset()),
                List.of(value));
    }

    private AstNode parseAssertStatement() {
        Token assertToken = stream.expect(JavaTokenType.KEYWORD, "assert");
        skipTrivia();
        AstNode condition = parseExpression();
        skipTrivia();
        AstNode message = null;
        if (stream.peek().type() == JavaTokenType.OPERATOR && stream.peek().text().equals(":")) {
            stream.consume();
            skipTrivia();
            message = parseExpression();
            skipTrivia();
        }
        Token semi = stream.expect(JavaTokenType.SEPARATOR, ";");
        List<AstNode> children = new ArrayList<>();
        children.add(AstNode.of(AstNodeKind.CONDITION, condition.range(), List.of(condition)));
        if (message != null) {
            children.add(message);
        }
        return AstNode.of(AstNodeKind.ASSERT_STATEMENT,
                TextRange.of(assertToken.startOffset(), semi.endOffset()), children);
    }

    private AstNode parseSwitchExpression() {
        Token switchToken = stream.expect(JavaTokenType.KEYWORD, "switch");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        skipTrivia();
        AstNode selector = parseExpression();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "{");
        List<AstNode> children = new ArrayList<>();
        children.add(AstNode.of(AstNodeKind.CONDITION, selector.range(), List.of(selector)));
        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                break;
            }
            if (stream.match(JavaTokenType.KEYWORD, "case")) {
                children.add(parseSwitchCase(stream.previous().startOffset(), true));
                continue;
            }
            if (stream.match(JavaTokenType.KEYWORD, "default")) {
                int defaultStart = stream.previous().startOffset();
                skipTrivia();
                stream.expect(JavaTokenType.OPERATOR, "->");
                int labelEnd = stream.previous().endOffset();
                AstNode label = AstNode.of(AstNodeKind.SWITCH_LABEL,
                        TextRange.of(defaultStart, labelEnd), List.of());
                children.add(parseSwitchCaseBody(TextRange.of(defaultStart, labelEnd), label, true));
                continue;
            }
            children.add(parseStatement());
        }
        return AstNode.of(AstNodeKind.SWITCH_EXPRESSION,
                TextRange.of(switchToken.startOffset(), stream.previous().endOffset()), children);
    }

    private void skipThrowsClause() {
        skipTrivia();
        if (!stream.match(JavaTokenType.KEYWORD, "throws")) {
            return;
        }

        while (stream.hasNext()) {
            skipTrivia();
            Token token = stream.peek();
            if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals("{") || token.text().equals(";"))) {
                return;
            }
            stream.consume();
        }
    }

    private List<JavaParameterModel> parseParameters(List<AstNode> paramNodes) {
        List<JavaParameterModel> parameters = new java.util.ArrayList<>();
        skipTrivia();

        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(")")) {
            return parameters;
        }

        while (stream.hasNext()) {
            int typeStart = stream.peek().startOffset();
            String type = parseTypeReference(false);
            int typeEnd = stream.previous().endOffset();
            TextRange typeRange = TextRange.of(typeStart, typeEnd);
            skipTrivia();
            Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);

            JavaParameterModel parameter = new JavaParameterModel();
            parameter.setType(type);
            parameter.setName(nameToken.text());
            parameter.setRange(TextRange.of(typeStart, nameToken.endOffset()));
            parameters.add(parameter);

            paramNodes.add(AstNode.of(AstNodeKind.PARAMETER,
                    TextRange.of(typeStart, nameToken.endOffset()),
                    List.of(AstNode.of(AstNodeKind.TYPE, typeRange, List.of()))));

            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
            skipTrivia();
        }

        return parameters;
    }

    private String parseTypeReference(boolean allowVoid) {
        StringBuilder sb = new StringBuilder();
        appendTypeName(sb, allowVoid);
        parseGenericArguments(sb);
        parseArraySuffix(sb);
        return sb.toString();
    }

    private boolean consumeType(boolean allowVoid) {
        int mark = stream.mark();
        StringBuilder ignored = new StringBuilder();
        if (!appendTypeName(ignored, allowVoid)) {
            stream.reset(mark);
            return false;
        }
        parseGenericArguments(ignored);
        parseArraySuffix(ignored);
        return true;
    }

    private boolean appendTypeName(StringBuilder sb, boolean allowVoid) {
        Token token = stream.peek();
        if (!isTypeToken(token, allowVoid)) {
            return false;
        }

        sb.append(stream.consume().text());
        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            Token next = stream.peek();
            if (!isTypeToken(next, false)) {
                break;
            }
            sb.append(stream.consume().text());
        }
        return true;
    }

    private void parseGenericArguments(StringBuilder sb) {
        int mark = stream.mark();
        skipTrivia();
        if (!stream.match(JavaTokenType.OPERATOR, "<")) {
            stream.reset(mark);
            return;
        }

        sb.append("<");
        int depth = 1;
        while (stream.hasNext() && depth > 0) {
            skipTrivia();
            Token token = stream.consume();
            sb.append(token.text());

            if (token.type() == JavaTokenType.OPERATOR && token.text().equals("<")) {
                depth++;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">")) {
                depth--;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">>")) {
                depth -= 2;
            } else if (token.type() == JavaTokenType.OPERATOR && token.text().equals(">>>")) {
                depth -= 3;
            }
        }
    }

    private void parseArraySuffix(StringBuilder sb) {
        while (true) {
            int mark = stream.mark();
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "[")) {
                skipTrivia();
                if (stream.match(JavaTokenType.SEPARATOR, "]")) {
                    sb.append("[]");
                    continue;
                }
            }
            stream.reset(mark);
            return;
        }
    }

    private boolean isTypeToken(Token token, boolean allowVoid) {
        if (token.type() == JavaTokenType.IDENTIFIER) return true;
        if (token.type() != JavaTokenType.KEYWORD) return false;
        String lexeme = token.text();
        return lexeme.equals("boolean") || lexeme.equals("byte") || lexeme.equals("char")
                || lexeme.equals("short") || lexeme.equals("int") || lexeme.equals("long")
                || lexeme.equals("float") || lexeme.equals("double")
                || lexeme.equals("var")
                || (allowVoid && lexeme.equals("void"));
    }

    private boolean isNestedType() {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        boolean result = detectTypeKind(stream.peek()) != null;
        stream.reset(mark);
        return result;
    }

    private void skipToBodyOrSemicolon() {
        while (stream.hasNext() && !stream.isEOF()) {
            Token current = stream.peek();
            if (current.type() == JavaTokenType.SEPARATOR && current.text().equals("{")) {
                return;
            }
            if (current.type() == JavaTokenType.SEPARATOR && current.text().equals(";")) {
                return;
            }
            stream.consume();
        }
    }

    private void skipTrivia() {
        while (stream.hasNext()) {
            com.eyecode.language.TokenType type = stream.peek().type();
            if (type == JavaTokenType.WHITESPACE || type == JavaTokenType.COMMENT) {
                stream.consume();
            } else {
                break;
            }
        }
    }

    private TypeKind detectTypeKind(Token token) {
        if (token.type() != JavaTokenType.KEYWORD) return null;
        return switch (token.text()) {
            case "class" -> TypeKind.CLASS;
            case "interface" -> TypeKind.INTERFACE;
            case "enum" -> TypeKind.ENUM;
            case "record" -> TypeKind.RECORD;
            default -> null;
        };
    }

    private boolean isModifierKeyword(Token token) {
        if (token.type() != JavaTokenType.KEYWORD) return false;
        String lexeme = token.text();
        return lexeme.equals("public") || lexeme.equals("private") || lexeme.equals("protected")
                || lexeme.equals("static") || lexeme.equals("final") || lexeme.equals("abstract")
                || lexeme.equals("transient") || lexeme.equals("volatile") || lexeme.equals("synchronized")
                || lexeme.equals("native") || lexeme.equals("strictfp") || lexeme.equals("default")
                || lexeme.equals("sealed");
    }

    private JavaModifier toModifier(String keyword) {
        return switch (keyword) {
            case "public" -> JavaModifier.PUBLIC;
            case "private" -> JavaModifier.PRIVATE;
            case "protected" -> JavaModifier.PROTECTED;
            case "static" -> JavaModifier.STATIC;
            case "final" -> JavaModifier.FINAL;
            case "abstract" -> JavaModifier.ABSTRACT;
            case "transient" -> JavaModifier.TRANSIENT;
            case "volatile" -> JavaModifier.VOLATILE;
            case "synchronized" -> JavaModifier.SYNCHRONIZED;
            case "native" -> JavaModifier.NATIVE;
            case "strictfp" -> JavaModifier.STRICTFP;
            case "default" -> JavaModifier.DEFAULT;
            default -> null;
        };
    }
}
