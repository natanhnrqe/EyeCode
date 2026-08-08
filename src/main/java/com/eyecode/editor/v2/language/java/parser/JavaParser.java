package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.language.Token;
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

import java.util.EnumSet;
import java.util.List;

public final class JavaParser {

    private final JavaTokenStream stream;

    public JavaParser(JavaTokenStream stream) {
        this.stream = stream;
    }

    public JavaFileModel parse() {
        JavaFileModel model = new JavaFileModel();
        skipTrivia();
        parsePackage(model);
        skipTrivia();
        parseImports(model);
        skipTrivia();
        parseTypes(model);
        return model;
    }

    private void parsePackage(JavaFileModel model) {
        skipTrivia();
        if (!stream.match(JavaTokenType.KEYWORD, "package")) {
            return;
        }

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
        model.setPackageName(sb.toString());
    }

    private void parseImports(JavaFileModel model) {
        skipTrivia();
        while (stream.peek().type() == JavaTokenType.KEYWORD
                && stream.peek().text().equals("import")) {
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
            model.getImports().add(sb.toString());
            skipTrivia();
        }
    }

    private void parseTypes(JavaFileModel model) {
        while (!stream.isEOF()) {
            parseType(model);
        }
    }

    private void parseType(JavaFileModel model) {
        skipTrivia();
        if (stream.isEOF()) return;

        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
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

        stream.consume();
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);

        JavaClassModel classModel = new JavaClassModel();
        classModel.setName(nameToken.text());
        classModel.setKind(kind);
        classModel.setModifiers(modifiers);

        parseTypeHeader(classModel);

        skipToBodyOrSemicolon();

        if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals("{")) {
            parseClassBody(classModel);
        } else if (stream.peek().type() == JavaTokenType.SEPARATOR
                && stream.peek().text().equals(";")) {
            stream.consume();
        }

        model.getTypes().add(classModel);
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

    private void parseClassBody(JavaClassModel model) {
        stream.expect(JavaTokenType.SEPARATOR, "{");

        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                return;
            }

            if (isNestedType()) {
                parseNestedType(model);
                continue;
            }

            if (isConstructor(model.getName())) {
                parseConstructor(model);
                continue;
            }

            if (isMethod()) {
                parseMethod(model);
                continue;
            }

            if (isField()) {
                parseField(model);
                continue;
            }

            skipMember();
        }
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

    private void parseConstructor(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER, owner.getName());
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<JavaParameterModel> parameters = parseParameters();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");

        skipMethodBody();

        JavaConstructorModel constructor = new JavaConstructorModel();
        constructor.setName(nameToken.text());
        constructor.setModifiers(modifiers);
        constructor.setParameters(parameters);
        constructor.setOwner(owner.getName());
        owner.getConstructors().add(constructor);
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

    private void parseMethod(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        String returnType = parseTypeReference(true);
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<JavaParameterModel> parameters = parseParameters();
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
        owner.getMethods().add(method);
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

    private void parseField(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        String type = parseTypeReference(false);
        skipTrivia();
        Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }

        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");

        JavaFieldModel field = new JavaFieldModel();
        field.setName(nameToken.text());
        field.setType(type);
        field.setModifiers(modifiers);
        field.setOwner(owner.getName());
        owner.getFields().add(field);
    }

    private void parseNestedType(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().text());
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

    private List<JavaParameterModel> parseParameters() {
        List<JavaParameterModel> parameters = new java.util.ArrayList<>();
        skipTrivia();

        if (stream.peek().type() == JavaTokenType.SEPARATOR && stream.peek().text().equals(")")) {
            return parameters;
        }

        while (stream.hasNext()) {
            String type = parseTypeReference(false);
            skipTrivia();
            Token nameToken = stream.expect(JavaTokenType.IDENTIFIER);

            JavaParameterModel parameter = new JavaParameterModel();
            parameter.setType(type);
            parameter.setName(nameToken.text());
            parameters.add(parameter);

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
        skipTrivia();
        if (!stream.match(JavaTokenType.OPERATOR, "<")) {
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
            skipTrivia();
            int mark = stream.mark();
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
