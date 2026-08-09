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

            skipMember();
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

        skipMethodBody();
        int declEnd = stream.previous().endOffset();

        JavaConstructorModel constructor = new JavaConstructorModel();
        constructor.setName(nameToken.text());
        constructor.setModifiers(modifiers);
        constructor.setParameters(parameters);
        constructor.setOwner(owner.getName());
        constructor.setRange(TextRange.of(declStart, declEnd));
        owner.getConstructors().add(constructor);

        members.add(memberNode(AstNodeKind.CONSTRUCTOR_DECLARATION,
                declStart, declEnd, modifierRanges, annotations, paramNodes));
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

        parseMethodBody(method);
        int declEnd = stream.previous().endOffset();
        method.setRange(TextRange.of(declStart, declEnd));
        owner.getMethods().add(method);

        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + paramNodes.size() + 1);
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.add(AstNode.of(AstNodeKind.TYPE, typeRange, List.of()));
        children.addAll(paramNodes);
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

        skipMember();
        int declEnd = stream.previous().endOffset();
        nestedType.setRange(TextRange.of(declStart, declEnd));

        List<AstNode> children = new ArrayList<>(modifierRanges.size() + annotations.size());
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        members.add(AstNode.of(kindNode(kind), TextRange.of(declStart, declEnd), children));
    }

    private static AstNode memberNode(AstNodeKind kind, int start, int end,
                                      List<TextRange> modifierRanges,
                                      List<TextRange> annotations,
                                      List<AstNode> paramNodes) {
        List<AstNode> children = new ArrayList<>(
                modifierRanges.size() + annotations.size() + paramNodes.size());
        for (TextRange annotationRange : annotations) {
            children.add(AstNode.of(AstNodeKind.ANNOTATION, annotationRange, List.of()));
        }
        for (TextRange modifierRange : modifierRanges) {
            children.add(AstNode.of(AstNodeKind.MODIFIER, modifierRange, List.of()));
        }
        children.addAll(paramNodes);
        return AstNode.of(kind, TextRange.of(start, end), children);
    }

    private void skipMember() {
        int depth = 0;

        while (stream.hasNext()) {
            Token token = stream.peek();

            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(";") && depth == 0) {
                stream.consume();
                return;
            }

            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}") && depth == 0) {
                return;
            }

            token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("{")) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}")) {
                depth--;
                if (depth <= 0) {
                    return;
                }
            }
        }
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

    private void skipMethodBody() {
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ";")) {
            return;
        }

        stream.expect(JavaTokenType.SEPARATOR, "{");
        int depth = 1;
        while (stream.hasNext() && depth > 0) {
            Token token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("{")) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("}")) {
                depth--;
            }
        }
    }

    private void parseMethodBody(JavaMethodModel method) {
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ";")) {
            return;
        }

        stream.expect(JavaTokenType.SEPARATOR, "{");
        int braceDepth = 1;

        while (stream.hasNext() && braceDepth > 0) {
            skipTrivia();

            if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals("}")) {
                stream.consume();
                braceDepth--;
                continue;
            }

            if (braceDepth == 1 && isLocalVariable()) {
                parseLocalVariable(method);
                continue;
            }

            Token token = stream.peek();
            if (token.type() == JavaTokenType.SEPARATOR && token.text().equals("{")) {
                stream.consume();
                braceDepth++;
                continue;
            }

            skipStatement();
        }
    }

    private boolean isLocalVariable() {
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
                        || (next.type() == JavaTokenType.SEPARATOR && next.text().equals(";"));
            }
        }

        stream.reset(mark);
        return result;
    }

    private void parseLocalVariable(JavaMethodModel method) {
        String type = parseTypeReference(false);
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }
        stream.expect(JavaTokenType.SEPARATOR, ";");

        JavaVariableModel variable = new JavaVariableModel();
        variable.setName(nameToken.text());
        variable.setType(type);
        variable.setOwnerMethod(method.getName());
        method.getLocalVariables().add(variable);
    }

    private void skipStatement() {
        int depth = 0;

        while (stream.hasNext()) {
            Token token = stream.peek();
            if (depth == 0) {
                if (token.type() == JavaTokenType.SEPARATOR && token.text().equals(";")) {
                    stream.consume();
                    return;
                }
                if (token.type() == JavaTokenType.SEPARATOR
                        && (token.text().equals("{") || token.text().equals("}"))) {
                    return;
                }
            }

            token = stream.consume();
            if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals("(") || token.text().equals("[") || token.text().equals("{"))) {
                depth++;
            } else if (token.type() == JavaTokenType.SEPARATOR
                    && (token.text().equals(")") || token.text().equals("]") || token.text().equals("}"))) {
                depth--;
                if (depth < 0) {
                    return;
                }
            }
        }
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
